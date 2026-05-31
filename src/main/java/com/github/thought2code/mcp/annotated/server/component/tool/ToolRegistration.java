package com.github.thought2code.mcp.annotated.server.component.tool;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.exception.McpServerComponentRegistrationException;
import com.github.thought2code.mcp.annotated.server.McpStructuredContent;
import com.github.thought2code.mcp.annotated.server.component.ComponentProvider;
import com.github.thought2code.mcp.annotated.util.JacksonHelper;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/** Registers build-time component {@code @McpTool} definitions. */
public final class ToolRegistration {

  private static final Logger log = LoggerFactory.getLogger(ToolRegistration.class);

  private ToolRegistration() {}

  /**
   * Registers all component tool definitions to a sync MCP server.
   *
   * @param server sync MCP server
   * @param context application context
   * @return {@code true} when at least one component tool was registered
   */
  public static boolean registerSync(McpSyncServer server, McpApplicationContext context) {
    return registerSync(server, context, ServiceLoader.load(ComponentProvider.class));
  }

  static boolean registerSync(
      McpSyncServer server, McpApplicationContext context, Iterable<ComponentProvider> providers) {
    List<ToolDefinition> definitions = loadDefinitions(providers, context);
    if (definitions.isEmpty()) {
      return false;
    }
    rejectDuplicateNames(definitions);
    for (ToolDefinition definition : definitions) {
      McpServerFeatures.SyncToolSpecification specification =
          McpServerFeatures.SyncToolSpecification.builder()
              .tool(definition.tool())
              .callHandler(
                  (exchange, request) ->
                      invoke(definition.invoker(), context, request, definition.sourceMethod()))
              .build();
      server.addTool(specification);
      log.debug("Sync McpTool {} registered successfully", definition.tool().name());
    }
    return true;
  }

  /**
   * Registers all component tool definitions to an async MCP server.
   *
   * @param server async MCP server
   * @param context application context
   * @return {@code true} when at least one component tool was registered
   */
  public static boolean registerAsync(McpAsyncServer server, McpApplicationContext context) {
    return registerAsync(server, context, ServiceLoader.load(ComponentProvider.class));
  }

  static boolean registerAsync(
      McpAsyncServer server, McpApplicationContext context, Iterable<ComponentProvider> providers) {
    List<ToolDefinition> definitions = loadDefinitions(providers, context);
    if (definitions.isEmpty()) {
      return false;
    }
    rejectDuplicateNames(definitions);
    for (ToolDefinition definition : definitions) {
      McpServerFeatures.AsyncToolSpecification specification =
          McpServerFeatures.AsyncToolSpecification.builder()
              .tool(definition.tool())
              .callHandler(
                  (exchange, request) ->
                      Mono.fromCallable(
                          () ->
                              invoke(
                                  definition.invoker(),
                                  context,
                                  request,
                                  definition.sourceMethod())))
              .build();
      Mono<Void> registration = server.addTool(specification);
      awaitAsyncRegistration(registration, definition.tool().name());
      log.debug("Async McpTool {} registered successfully", definition.tool().name());
    }
    return true;
  }

  private static List<ToolDefinition> loadDefinitions(
      Iterable<ComponentProvider> providers, McpApplicationContext context) {
    List<ToolDefinition> definitions = new ArrayList<>();
    for (ComponentProvider provider : providers) {
      for (ToolDefinition definition : provider.tools()) {
        if (context.isInScope(definition.sourceMethod())) {
          definitions.add(definition);
        }
      }
    }
    return definitions;
  }

  private static void rejectDuplicateNames(List<ToolDefinition> definitions) {
    Map<String, String> registeredNames = new HashMap<>();
    for (ToolDefinition definition : definitions) {
      final String name = definition.tool().name();
      String previous = registeredNames.putIfAbsent(name, definition.sourceMethod());
      if (previous != null) {
        throw new McpServerComponentRegistrationException(
            String.format(
                "Duplicate McpTool name '%s' found for methods %s and %s",
                name, previous, definition.sourceMethod()));
      }
    }
  }

  private static McpSchema.CallToolResult invoke(
      ToolInvoker invoker,
      McpApplicationContext context,
      McpSchema.CallToolRequest request,
      String sourceMethod) {
    log.debug(
        "Handling component MCP CallToolRequest for {}: {}",
        sourceMethod,
        JacksonHelper.toJsonString(request));

    var invocation = invoker.invoke(context, request.arguments());
    Object result = invocation.result();
    String textContent = invocation.asText();
    Object structuredContent = Map.of();
    if (!invocation.isError() && result instanceof McpStructuredContent mcpStructuredContent) {
      textContent = mcpStructuredContent.asTextContent();
      structuredContent = mcpStructuredContent;
    }

    McpSchema.CallToolResult callToolResult =
        McpSchema.CallToolResult.builder()
            .addTextContent(textContent)
            .structuredContent(structuredContent)
            .isError(invocation.isError())
            .build();
    log.debug(
        "Returning component MCP CallToolResult for {}: {}",
        sourceMethod,
        JacksonHelper.toJsonString(callToolResult));
    return callToolResult;
  }

  private static void awaitAsyncRegistration(Mono<Void> registration, String specificationName) {
    try {
      registration.block();
    } catch (RuntimeException e) {
      final String message =
          String.format("Failed to register async McpTool %s", specificationName);
      log.error(message, e);
      throw new McpServerComponentRegistrationException(message, e);
    }
  }
}
