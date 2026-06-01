package com.github.thought2code.mcp.annotated.server.component.tool;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.server.McpStructuredContent;
import com.github.thought2code.mcp.annotated.server.component.ComponentRegistrationSupport;
import com.github.thought2code.mcp.annotated.server.component.ComponentProvider;
import com.github.thought2code.mcp.annotated.server.component.DuplicateComponentMessageHelper;
import com.github.thought2code.mcp.annotated.util.JacksonHelper;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
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
    List<ToolDefinition> definitions =
        ComponentRegistrationSupport.prepareDefinitions(
            providers,
            context,
            ComponentProvider::tools,
            ToolDefinition::sourceMethod,
            ToolRegistration::toolName,
            DuplicateComponentMessageHelper::duplicateToolName);
    return ComponentRegistrationSupport.registerSyncDefinitions(
        definitions,
        definition ->
            server.addTool(
                McpServerFeatures.SyncToolSpecification.builder()
                    .tool(definition.tool())
                    .callHandler(
                        (exchange, request) ->
                            invoke(definition.invoker(), context, request, definition.sourceMethod()))
                    .build()),
        ToolRegistration::logSyncRegistered);
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
    List<ToolDefinition> definitions =
        ComponentRegistrationSupport.prepareDefinitions(
            providers,
            context,
            ComponentProvider::tools,
            ToolDefinition::sourceMethod,
            ToolRegistration::toolName,
            DuplicateComponentMessageHelper::duplicateToolName);
    return ComponentRegistrationSupport.registerAsyncDefinitions(
        definitions,
        definition ->
            server.addTool(
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
                    .build()),
        ToolRegistration::toolName,
        "McpTool",
        log,
        ToolRegistration::logAsyncRegistered);
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

  private static String toolName(ToolDefinition definition) {
    return definition.tool().name();
  }

  private static void logSyncRegistered(ToolDefinition definition) {
    log.debug("Sync McpTool {} registered successfully", toolName(definition));
  }

  private static void logAsyncRegistered(ToolDefinition definition) {
    log.debug("Async McpTool {} registered successfully", toolName(definition));
  }

}
