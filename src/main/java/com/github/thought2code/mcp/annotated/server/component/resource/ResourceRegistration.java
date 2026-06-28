package com.github.thought2code.mcp.annotated.server.component.resource;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.enums.MimeType;
import com.github.thought2code.mcp.annotated.server.component.ComponentProvider;
import com.github.thought2code.mcp.annotated.server.component.ComponentRegistrationSupport;
import com.github.thought2code.mcp.annotated.server.component.DuplicateComponentMessageHelper;
import com.github.thought2code.mcp.annotated.util.JacksonHelper;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.ServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/**
 * Registers build-time {@code @McpResource} definitions with sync or async MCP servers.
 *
 * @author codeboyzhou
 */
public final class ResourceRegistration {

  private static final Logger log = LoggerFactory.getLogger(ResourceRegistration.class);

  private ResourceRegistration() {}

  /**
   * Registers all in-scope resources on a sync MCP server.
   *
   * @param server sync MCP server
   * @param context application context
   * @return {@code true} when at least one resource was registered
   */
  public static boolean registerSync(McpSyncServer server, McpApplicationContext context) {
    return registerSync(server, context, ServiceLoader.load(ComponentProvider.class));
  }

  /** Registers resources using an explicit provider iterable (for tests). */
  static boolean registerSync(
      McpSyncServer server, McpApplicationContext context, Iterable<ComponentProvider> providers) {
    List<ResourceDefinition> definitions =
        ComponentRegistrationSupport.prepareDefinitions(
            providers,
            context,
            ComponentProvider::resources,
            ResourceDefinition::sourceMethod,
            ResourceRegistration::resourceName,
            DuplicateComponentMessageHelper::duplicateResourceName);
    return ComponentRegistrationSupport.registerSyncDefinitions(
        definitions,
        definition ->
            server.addResource(
                new McpServerFeatures.SyncResourceSpecification(
                    definition.resource(),
                    (exchange, request) ->
                        invoke(
                            definition.invoker(),
                            context,
                            definition.resource(),
                            definition.sourceMethod()))),
        ResourceRegistration::logSyncRegistered);
  }

  /**
   * Registers all in-scope resources on an async MCP server.
   *
   * @param server async MCP server
   * @param context application context
   * @return {@code true} when at least one resource was registered
   */
  public static boolean registerAsync(McpAsyncServer server, McpApplicationContext context) {
    return registerAsync(server, context, ServiceLoader.load(ComponentProvider.class));
  }

  /** Registers resources asynchronously using an explicit provider iterable (for tests). */
  static boolean registerAsync(
      McpAsyncServer server, McpApplicationContext context, Iterable<ComponentProvider> providers) {
    List<ResourceDefinition> definitions =
        ComponentRegistrationSupport.prepareDefinitions(
            providers,
            context,
            ComponentProvider::resources,
            ResourceDefinition::sourceMethod,
            ResourceRegistration::resourceName,
            DuplicateComponentMessageHelper::duplicateResourceName);
    return ComponentRegistrationSupport.registerAsyncDefinitions(
        definitions,
        definition ->
            server.addResource(
                new McpServerFeatures.AsyncResourceSpecification(
                    definition.resource(),
                    (exchange, request) ->
                        Mono.fromCallable(
                            () ->
                                invoke(
                                    definition.invoker(),
                                    context,
                                    definition.resource(),
                                    definition.sourceMethod())))),
        ResourceRegistration::resourceName,
        "McpResource",
        log,
        ResourceRegistration::logAsyncRegistered);
  }

  /** Maps a {@link ResourceInvoker} result to an MCP {@link McpSchema.ReadResourceResult}. */
  private static McpSchema.ReadResourceResult invoke(
      ResourceInvoker invoker,
      McpApplicationContext context,
      McpSchema.Resource resource,
      String sourceMethod) {
    log.debug(
        "Handling component ReadResource request for {}: {}",
        sourceMethod,
        JacksonHelper.toJsonString(resource));

    var invocation = invoker.invoke(context);
    final String uri = resource.uri();
    final String mimeType = resource.mimeType();
    final String text =
        MimeType.APPLICATION_JSON.getValue().equals(mimeType)
            ? invocation.asJson()
            : invocation.asText();
    McpSchema.ResourceContents contents =
        McpSchema.TextResourceContents.builder(uri, text).mimeType(mimeType).build();
    McpSchema.ReadResourceResult result =
        McpSchema.ReadResourceResult.builder(List.of(contents)).build();

    log.debug(
        "Returning component ReadResourceResult for {}: {}",
        sourceMethod,
        JacksonHelper.toJsonString(result));
    return result;
  }

  /** Returns the MCP resource name from a definition. */
  private static String resourceName(ResourceDefinition definition) {
    return definition.resource().name();
  }

  /** Logs successful sync registration of one resource. */
  private static void logSyncRegistered(ResourceDefinition definition) {
    log.debug("Sync McpResource {} registered successfully", resourceName(definition));
  }

  /** Logs successful async registration of one resource. */
  private static void logAsyncRegistered(ResourceDefinition definition) {
    log.debug("Async McpResource {} registered successfully", resourceName(definition));
  }
}
