package com.github.thought2code.mcp.annotated.compiled.resource;

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

/** Registers build-time compiled {@code @McpResource} definitions. */
public final class CompiledResourceRegistration {

  private static final Logger log = LoggerFactory.getLogger(CompiledResourceRegistration.class);

  private CompiledResourceRegistration() {}

  public static boolean registerSync(McpSyncServer server, McpApplicationContext context) {
    return registerSync(server, context, ServiceLoader.load(McpCompiledModelProvider.class));
  }

  static boolean registerSync(
      McpSyncServer server,
      McpApplicationContext context,
      Iterable<McpCompiledModelProvider> providers) {
    List<CompiledResourceDefinition> definitions = loadDefinitions(providers, context);
    if (definitions.isEmpty()) {
      return false;
    }
    rejectDuplicateNames(definitions);
    for (CompiledResourceDefinition definition : definitions) {
      McpServerFeatures.SyncResourceSpecification specification =
          new McpServerFeatures.SyncResourceSpecification(
              definition.resource(),
              (exchange, request) ->
                  invoke(
                      definition.invoker(),
                      context,
                      definition.resource(),
                      definition.sourceMethod()));
      server.addResource(specification);
      log.debug(
          "Compiled sync McpResource {} registered successfully", definition.resource().name());
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
    List<CompiledResourceDefinition> definitions = loadDefinitions(providers, context);
    if (definitions.isEmpty()) {
      return false;
    }
    rejectDuplicateNames(definitions);
    for (CompiledResourceDefinition definition : definitions) {
      McpServerFeatures.AsyncResourceSpecification specification =
          new McpServerFeatures.AsyncResourceSpecification(
              definition.resource(),
              (exchange, request) ->
                  Mono.fromCallable(
                      () ->
                          invoke(
                              definition.invoker(),
                              context,
                              definition.resource(),
                              definition.sourceMethod())));
      Mono<Void> registration = server.addResource(specification);
      awaitAsyncRegistration(registration, definition.resource().name());
      log.debug(
          "Compiled async McpResource {} registered successfully", definition.resource().name());
    }
    return true;
  }

  private static List<CompiledResourceDefinition> loadDefinitions(
      Iterable<McpCompiledModelProvider> providers, McpApplicationContext context) {
    List<CompiledResourceDefinition> definitions = new ArrayList<>();
    for (McpCompiledModelProvider provider : providers) {
      for (CompiledResourceDefinition definition : provider.resources()) {
        if (context.isInScope(definition.sourceMethod())) {
          definitions.add(definition);
        }
      }
    }
    return definitions;
  }

  private static void rejectDuplicateNames(List<CompiledResourceDefinition> definitions) {
    Map<String, String> registeredNames = new HashMap<>();
    for (CompiledResourceDefinition definition : definitions) {
      final String name = definition.resource().name();
      String previous = registeredNames.putIfAbsent(name, definition.sourceMethod());
      if (previous != null) {
        throw new McpServerComponentRegistrationException(
            String.format(
                "Duplicate McpResource name '%s' found for methods %s and %s",
                name, previous, definition.sourceMethod()));
      }
    }
  }

  private static McpSchema.ReadResourceResult invoke(
      CompiledResourceInvoker invoker,
      McpApplicationContext context,
      McpSchema.Resource resource,
      String sourceMethod) {
    log.debug(
        "Handling compiled ReadResource request for {}: {}",
        sourceMethod,
        JacksonHelper.toJsonString(resource));

    var invocation = invoker.invoke(context);
    final String uri = resource.uri();
    final String mimeType = resource.mimeType();
    final String text = invocation.asText();
    McpSchema.ResourceContents contents =
        McpSchema.TextResourceContents.builder(uri, text).mimeType(mimeType).build();
    McpSchema.ReadResourceResult result =
        McpSchema.ReadResourceResult.builder(List.of(contents)).build();

    log.debug(
        "Returning compiled ReadResourceResult for {}: {}",
        sourceMethod,
        JacksonHelper.toJsonString(result));
    return result;
  }

  private static void awaitAsyncRegistration(Mono<Void> registration, String specificationName) {
    try {
      registration.block();
    } catch (RuntimeException e) {
      final String message =
          String.format("Failed to register async McpResource %s", specificationName);
      log.error(message, e);
      throw new McpServerComponentRegistrationException(message, e);
    }
  }
}
