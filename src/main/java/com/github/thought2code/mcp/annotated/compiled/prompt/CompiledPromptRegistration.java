package com.github.thought2code.mcp.annotated.compiled.prompt;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.compiled.spi.McpCompiledModelProvider;
import com.github.thought2code.mcp.annotated.exception.McpServerComponentRegistrationException;
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

/** Registers build-time compiled {@code @McpPrompt} definitions. */
public final class CompiledPromptRegistration {

  private static final Logger log = LoggerFactory.getLogger(CompiledPromptRegistration.class);

  private CompiledPromptRegistration() {}

  public static boolean registerSync(McpSyncServer server, McpApplicationContext context) {
    return registerSync(server, context, ServiceLoader.load(McpCompiledModelProvider.class));
  }

  static boolean registerSync(
      McpSyncServer server,
      McpApplicationContext context,
      Iterable<McpCompiledModelProvider> providers) {
    List<CompiledPromptDefinition> definitions = loadDefinitions(providers, context);
    if (definitions.isEmpty()) {
      return false;
    }
    rejectDuplicateNames(definitions);
    for (CompiledPromptDefinition definition : definitions) {
      McpServerFeatures.SyncPromptSpecification specification =
          new McpServerFeatures.SyncPromptSpecification(
              definition.prompt(),
              (exchange, request) ->
                  invoke(
                      definition.invoker(),
                      context,
                      definition.description(),
                      request,
                      definition.sourceMethod()));
      server.addPrompt(specification);
      log.debug("Compiled sync McpPrompt {} registered successfully", definition.prompt().name());
    }
    return true;
  }

  public static boolean registerAsync(McpAsyncServer server, McpApplicationContext context) {
    return registerAsync(server, context, ServiceLoader.load(McpCompiledModelProvider.class));
  }

  static boolean registerAsync(
      McpAsyncServer server,
      McpApplicationContext context,
      Iterable<McpCompiledModelProvider> providers) {
    List<CompiledPromptDefinition> definitions = loadDefinitions(providers, context);
    if (definitions.isEmpty()) {
      return false;
    }
    rejectDuplicateNames(definitions);
    for (CompiledPromptDefinition definition : definitions) {
      McpServerFeatures.AsyncPromptSpecification specification =
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
                              definition.sourceMethod())));
      Mono<Void> registration = server.addPrompt(specification);
      awaitAsyncRegistration(registration, definition.prompt().name());
      log.debug("Compiled async McpPrompt {} registered successfully", definition.prompt().name());
    }
    return true;
  }

  private static List<CompiledPromptDefinition> loadDefinitions(
      Iterable<McpCompiledModelProvider> providers, McpApplicationContext context) {
    List<CompiledPromptDefinition> definitions = new ArrayList<>();
    for (McpCompiledModelProvider provider : providers) {
      for (CompiledPromptDefinition definition : provider.prompts()) {
        if (context.isInScope(definition.sourceMethod())) {
          definitions.add(definition);
        }
      }
    }
    return definitions;
  }

  private static void rejectDuplicateNames(List<CompiledPromptDefinition> definitions) {
    Map<String, String> registeredNames = new HashMap<>();
    for (CompiledPromptDefinition definition : definitions) {
      final String name = definition.prompt().name();
      String previous = registeredNames.putIfAbsent(name, definition.sourceMethod());
      if (previous != null) {
        throw new McpServerComponentRegistrationException(
            String.format(
                "Duplicate McpPrompt name '%s' found for methods %s and %s",
                name, previous, definition.sourceMethod()));
      }
    }
  }

  private static McpSchema.GetPromptResult invoke(
      CompiledPromptInvoker invoker,
      McpApplicationContext context,
      String description,
      McpSchema.GetPromptRequest request,
      String sourceMethod) {
    log.debug(
        "Handling compiled MCP GetPromptRequest for {}: {}",
        sourceMethod,
        JacksonHelper.toJsonString(request));

    var invocation = invoker.invoke(context, request.arguments());
    McpSchema.Content content = McpSchema.TextContent.builder(invocation.asText()).build();
    McpSchema.PromptMessage message =
        McpSchema.PromptMessage.builder(McpSchema.Role.USER, content).build();
    McpSchema.GetPromptResult result =
        McpSchema.GetPromptResult.builder(List.of(message)).description(description).build();

    log.debug(
        "Returning compiled MCP GetPromptResult for {}: {}",
        sourceMethod,
        JacksonHelper.toJsonString(result));
    return result;
  }

  private static void awaitAsyncRegistration(Mono<Void> registration, String specificationName) {
    try {
      registration.block();
    } catch (RuntimeException e) {
      final String message =
          String.format("Failed to register async McpPrompt %s", specificationName);
      log.error(message, e);
      throw new McpServerComponentRegistrationException(message, e);
    }
  }
}
