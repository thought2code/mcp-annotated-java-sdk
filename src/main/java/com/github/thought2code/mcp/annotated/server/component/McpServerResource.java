package com.github.thought2code.mcp.annotated.server.component;

import com.github.thought2code.mcp.annotated.annotation.McpResource;
import com.github.thought2code.mcp.annotated.reflect.Invocation;
import com.github.thought2code.mcp.annotated.reflect.MethodCache;
import com.github.thought2code.mcp.annotated.reflect.MethodInvoker;
import com.github.thought2code.mcp.annotated.reflect.ReflectionsProvider;
import com.github.thought2code.mcp.annotated.util.JacksonHelper;
import com.github.thought2code.mcp.annotated.util.StringHelper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MCP server component for handling resource-related operations.
 *
 * <p>This class extends {@link McpServerComponentBase} and implements the functionality for
 * creating and registering resource components with an MCP server. It processes methods annotated
 * with {@link McpResource} and creates appropriate resource specifications that can be used to
 * expose data to LLM interactions.
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
    extends McpServerComponentBase<McpServerFeatures.SyncResourceSpecification>
    implements McpComponentRegistrar {

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
   * @return a synchronous resource specification for the MCP server
   * @see McpResource
   * @see McpSchema.Resource
   * @see McpSchema.Annotations
   */
  @Override
  public McpServerFeatures.SyncResourceSpecification from(Method method) {
    log.info("Creating resource specification for method: {}", method.toGenericString());

    // Use reflection cache for performance optimization
    MethodCache methodCache = MethodCache.of(method);
    Object instance = MethodInvoker.createInstance(methodCache.getDeclaringClass());

    McpResource res = methodCache.getMcpResourceAnnotation();
    final String name = StringHelper.defaultIfBlank(res.name(), methodCache.getMethodName());
    final String title = localizeAttribute(res.title(), name);
    final String description = localizeAttribute(res.description(), name);

    McpSchema.Resource resource =
        McpSchema.Resource.builder()
            .uri(res.uri())
            .name(name)
            .title(title)
            .description(description)
            .mimeType(res.mimeType())
            .annotations(new McpSchema.Annotations(List.of(res.roles()), res.priority()))
            .build();

    log.info("Resource specification created: {}", JacksonHelper.toJsonString(resource));

    return new McpServerFeatures.SyncResourceSpecification(
        resource, (exchange, request) -> invoke(instance, methodCache, resource));
  }

  /**
   * Registers all discovered components of this type with the given MCP server.
   *
   * <p>This method scans for methods annotated with the appropriate annotation(s) for this
   * component type and registers them with the server. The exact discovery and registration
   * mechanism depends on the implementation.
   *
   * @param server the {@link McpSyncServer} instance to register the components with; must not be
   *     {@code null}
   */
  @Override
  public void register(McpSyncServer server) {
    Set<Method> methods = ReflectionsProvider.getMethodsAnnotatedWith(McpResource.class);
    methods.forEach(
        method -> {
          log.debug("Registering resource method: {}", method.toGenericString());
          McpServerFeatures.SyncResourceSpecification resource = from(method);
          server.addResource(resource);
          log.debug("Resource {} registered successfully", resource.resource().name());
        });
  }

  /**
   * Invokes the resource method with the specified resource.
   *
   * <p>This private method handles the actual invocation of the resource method, using reflection
   * to call the method and wrapping the result in a {@link McpSchema.ReadResourceResult}. The
   * resource URI and MIME type are extracted from the resource specification.
   *
   * @param instance the object instance containing the resource method
   * @param methodCache the cached method information for efficient invocation
   * @param resource the resource specification containing URI and MIME type
   * @return the result of the resource invocation
   * @see McpSchema.ReadResourceResult
   * @see McpSchema.ResourceContents
   * @see McpSchema.TextResourceContents
   */
  private McpSchema.ReadResourceResult invoke(
      Object instance, MethodCache methodCache, McpSchema.Resource resource) {

    log.debug("Handling ReadResourceResult request: {}", JacksonHelper.toJsonString(resource));

    Invocation invocation = MethodInvoker.invoke(instance, methodCache);
    final String uri = resource.uri();
    final String mimeType = resource.mimeType();
    final String text = invocation.result().toString();
    McpSchema.ResourceContents contents = new McpSchema.TextResourceContents(uri, mimeType, text);
    McpSchema.ReadResourceResult readResourceResult =
        new McpSchema.ReadResourceResult(List.of(contents));

    log.debug("Returning ReadResourceResult: {}", JacksonHelper.toJsonString(readResourceResult));

    return readResourceResult;
  }
}
