package com.github.thought2code.mcp.annotated.server.component;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.annotation.McpResource;
import com.github.thought2code.mcp.annotated.reflect.Invocation;
import com.github.thought2code.mcp.annotated.reflect.MethodInvoker;
import com.github.thought2code.mcp.annotated.reflect.MethodMetadata;
import com.github.thought2code.mcp.annotated.util.JacksonHelper;
import com.github.thought2code.mcp.annotated.util.StringHelper;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import java.lang.reflect.Method;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/**
 * MCP server component for handling resource-related operations.
 *
 * <p>This class implements the functionality for creating and registering resource components with
 * an MCP server. It processes methods annotated with {@link McpResource} and creates appropriate
 * resource specifications that can be used to expose data to LLM interactions.
 *
 * <p>The class handles:
 *
 * <ul>
 *   <li>Creation of resource specifications from annotated methods
 *   <li>Registration of all resource components with the server
 *   <li>Invocation of resource methods to retrieve data
 *   <li>Localization of resource attributes using resource bundles
 * </ul>
 *
 * @author codeboyzhou
 * @see McpResource
 * @see McpSchema.Resource
 * @see McpSchema.ResourceContents
 */
public class McpServerResource
    extends AbstractDualModeComponentRegistrar<
        McpServerFeatures.SyncResourceSpecification, McpServerFeatures.AsyncResourceSpecification>
    implements McpServerComponent<McpServerFeatures.SyncResourceSpecification> {

  private static final Logger log = LoggerFactory.getLogger(McpServerResource.class);

  /**
   * Creates a synchronous resource specification from the specified method.
   *
   * <p>This method processes a method annotated with {@link McpResource} and creates a {@link
   * McpServerFeatures.SyncResourceSpecification} that can be registered with the MCP server. The
   * method extracts resource information from annotations and method signature, and builds a
   * resource specification with appropriate metadata.
   *
   * @param method the method annotated with {@link McpResource} to create a specification from
   * @param context the application context for component discovery and localization
   * @return a synchronous resource specification for the MCP server
   * @see McpResource
   * @see McpSchema.Resource
   * @see McpSchema.Annotations
   */
  @Override
  public McpServerFeatures.SyncResourceSpecification from(
      Method method, McpApplicationContext context) {
    log.info("Creating sync resource specification for method: {}", method.toGenericString());
    ResourceDefinition definition = createResourceDefinition(method, context, "Sync");
    return new McpServerFeatures.SyncResourceSpecification(
        definition.resource(),
        (exchange, request) -> invoke(definition.instance(), method, definition.resource()));
  }

  /**
   * Creates an asynchronous resource specification from the specified method.
   *
   * <p>This method processes a method annotated with {@link McpResource} and creates a {@link
   * McpServerFeatures.AsyncResourceSpecification} that can be registered with the MCP async server.
   * The handler wraps the synchronous invocation result in a {@link Mono}.
   *
   * @param method the method annotated with {@link McpResource} to create a specification from
   * @param context the application context for component discovery and localization
   * @return an asynchronous resource specification for the MCP server
   */
  public McpServerFeatures.AsyncResourceSpecification fromAsync(
      Method method, McpApplicationContext context) {
    log.info("Creating async resource specification for method: {}", method.toGenericString());
    ResourceDefinition definition = createResourceDefinition(method, context, "Async");
    return new McpServerFeatures.AsyncResourceSpecification(
        definition.resource(),
        (exchange, request) ->
            Mono.fromCallable(() -> invoke(definition.instance(), method, definition.resource())));
  }

  @Override
  protected Class<McpResource> annotationType() {
    return McpResource.class;
  }

  @Override
  protected McpServerFeatures.SyncResourceSpecification createSyncSpecification(
      Method method, McpApplicationContext context) {
    return from(method, context);
  }

  @Override
  protected McpServerFeatures.AsyncResourceSpecification createAsyncSpecification(
      Method method, McpApplicationContext context) {
    return fromAsync(method, context);
  }

  @Override
  protected void addSyncSpecification(
      McpSyncServer server, McpServerFeatures.SyncResourceSpecification specification) {
    server.addResource(specification);
  }

  @Override
  protected void addAsyncSpecification(
      McpAsyncServer server, McpServerFeatures.AsyncResourceSpecification specification) {
    server.addResource(specification).subscribe();
  }

  @Override
  protected String syncSpecificationName(
      McpServerFeatures.SyncResourceSpecification specification) {
    return specification.resource().name();
  }

  @Override
  protected String asyncSpecificationName(
      McpServerFeatures.AsyncResourceSpecification specification) {
    return specification.resource().name();
  }

  /**
   * Invokes the resource method with the specified resource.
   *
   * <p>This private method handles the actual invocation of the resource method, using reflection
   * to call the method and wrapping the result in a {@link McpSchema.ReadResourceResult}. The
   * resource URI and MIME type are extracted from the resource specification.
   *
   * @param instance the object instance containing the resource method
   * @param method the method to invoke
   * @param resource the resource specification containing URI and MIME type
   * @return the result of the resource invocation
   * @see McpSchema.ReadResourceResult
   * @see McpSchema.ResourceContents
   * @see McpSchema.TextResourceContents
   */
  private McpSchema.ReadResourceResult invoke(
      Object instance, Method method, McpSchema.Resource resource) {

    log.debug("Handling ReadResourceResult request: {}", JacksonHelper.toJsonString(resource));

    MethodMetadata metadata = MethodMetadata.of(method);
    Invocation invocation = MethodInvoker.invoke(instance, method, metadata);
    final String uri = resource.uri();
    final String mimeType = resource.mimeType();
    final String text = invocation.result().toString();
    McpSchema.ResourceContents contents = new McpSchema.TextResourceContents(uri, mimeType, text);
    McpSchema.ReadResourceResult readResourceResult =
        new McpSchema.ReadResourceResult(List.of(contents));

    log.debug("Returning ReadResourceResult: {}", JacksonHelper.toJsonString(readResourceResult));

    return readResourceResult;
  }

  /**
   * Creates the shared resource definition used by sync and async specifications.
   *
   * @param method annotated resource method
   * @param context application context
   * @param mode log label (Sync/Async)
   * @return resource definition containing metadata and target instance
   */
  private ResourceDefinition createResourceDefinition(
      Method method, McpApplicationContext context, String mode) {
    McpResource mcpResource = method.getAnnotation(McpResource.class);
    final String name = StringHelper.defaultIfBlank(mcpResource.name(), method.getName());
    final String title = context.getLocalizedString(mcpResource.title(), name);
    final String description = context.getLocalizedString(mcpResource.description(), name);
    McpSchema.Resource resource =
        McpSchema.Resource.builder()
            .uri(mcpResource.uri())
            .name(name)
            .title(title)
            .description(description)
            .mimeType(mcpResource.mimeType())
            .annotations(
                new McpSchema.Annotations(List.of(mcpResource.roles()), mcpResource.priority()))
            .build();
    log.info("{} resource specification created: {}", mode, JacksonHelper.toJsonString(resource));
    Object instance = context.getComponentInstance(method.getDeclaringClass());
    return new ResourceDefinition(resource, instance);
  }

  /** Resource definition containing metadata and target instance. */
  private record ResourceDefinition(McpSchema.Resource resource, Object instance) {}
}
