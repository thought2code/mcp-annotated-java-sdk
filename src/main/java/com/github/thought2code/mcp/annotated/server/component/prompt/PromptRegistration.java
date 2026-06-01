package com.github.thought2code.mcp.annotated.server.component.prompt;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.server.component.ComponentRegistrationSupport;
import com.github.thought2code.mcp.annotated.server.component.ComponentProvider;
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

/** Registers build-time component {@code @McpPrompt} definitions. */
public final class PromptRegistration {

  private static final Logger log = LoggerFactory.getLogger(PromptRegistration.class);

  private PromptRegistration() {}

  public static boolean registerSync(McpSyncServer server, McpApplicationContext context) {
    return registerSync(server, context, ServiceLoader.load(ComponentProvider.class));
  }

  static boolean registerSync(
      McpSyncServer server, McpApplicationContext context, Iterable<ComponentProvider> providers) {
    List<PromptDefinition> definitions =
        ComponentRegistrationSupport.prepareDefinitions(
            providers,
            context,
            ComponentProvider::prompts,
            PromptDefinition::sourceMethod,
            PromptRegistration::promptName,
            DuplicateComponentMessageHelper::duplicatePromptName);
    return ComponentRegistrationSupport.registerSyncDefinitions(
        definitions,
        definition ->
            server.addPrompt(
                new McpServerFeatures.SyncPromptSpecification(
                    definition.prompt(),
                    (exchange, request) ->
                        invoke(
                            definition.invoker(),
                            context,
                            definition.description(),
                            request,
                            definition.sourceMethod()))),
        PromptRegistration::logSyncRegistered);
  }

  public static boolean registerAsync(McpAsyncServer server, McpApplicationContext context) {
    return registerAsync(server, context, ServiceLoader.load(ComponentProvider.class));
  }

  static boolean registerAsync(
      McpAsyncServer server, McpApplicationContext context, Iterable<ComponentProvider> providers) {
    List<PromptDefinition> definitions =
        ComponentRegistrationSupport.prepareDefinitions(
            providers,
            context,
            ComponentProvider::prompts,
            PromptDefinition::sourceMethod,
            PromptRegistration::promptName,
            DuplicateComponentMessageHelper::duplicatePromptName);
    return ComponentRegistrationSupport.registerAsyncDefinitions(
        definitions,
        definition ->
            server.addPrompt(
                new McpServerFeatures.AsyncPromptSpecification(
                    definition.prompt(),
                    (exchange, request) ->
                        Mono.fromCallable(
                            () ->
                                invoke(
                                    definition.invoker(),
                                    context,
                                    definition.description(),
                                    request,
                                    definition.sourceMethod())))),
        PromptRegistration::promptName,
        "McpPrompt",
        log,
        PromptRegistration::logAsyncRegistered);
  }

  private static McpSchema.GetPromptResult invoke(
      PromptInvoker invoker,
      McpApplicationContext context,
      String description,
      McpSchema.GetPromptRequest request,
      String sourceMethod) {
    log.debug(
        "Handling component MCP GetPromptRequest for {}: {}",
        sourceMethod,
        JacksonHelper.toJsonString(request));

    var invocation = invoker.invoke(context, request.arguments());
    McpSchema.Content content = McpSchema.TextContent.builder(invocation.asText()).build();
    McpSchema.PromptMessage message =
        McpSchema.PromptMessage.builder(McpSchema.Role.USER, content).build();
    McpSchema.GetPromptResult result =
        McpSchema.GetPromptResult.builder(List.of(message)).description(description).build();

    log.debug(
        "Returning component MCP GetPromptResult for {}: {}",
        sourceMethod,
        JacksonHelper.toJsonString(result));
    return result;
  }

  private static String promptName(PromptDefinition definition) {
    return definition.prompt().name();
  }

  private static void logSyncRegistered(PromptDefinition definition) {
    log.debug("Sync McpPrompt {} registered successfully", promptName(definition));
  }

  private static void logAsyncRegistered(PromptDefinition definition) {
    log.debug("Async McpPrompt {} registered successfully", promptName(definition));
  }

}
