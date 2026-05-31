package com.github.thought2code.mcp.annotated.server.component.resource;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.exception.McpServerComponentRegistrationException;
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

/** Registers build-time component {@code @McpResource} definitions. */
public final class ResourceRegistration {

  private static final Logger log = LoggerFactory.getLogger(ResourceRegistration.class);

  private ResourceRegistration() {}

  public static boolean registerSync(McpSyncServer server, McpApplicationContext context) {
    return registerSync(server, context, ServiceLoader.load(ComponentProvider.class));
  }

  static boolean registerSync(
      McpSyncServer server, McpApplicationContext context, Iterable<ComponentProvider> providers) {
    List<ResourceDefinition> definitions = loadDefinitions(providers, context);
    if (definitions.isEmpty()) {
      return false;
    }
    rejectDuplicateNames(definitions);
    for (ResourceDefinition definition : definitions) {
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
      log.debug("Sync McpResource {} registered successfully", definition.resource().name());
    }
    return true;
  }

  public static boolean registerAsync(McpAsyncServer server, McpApplicationContext context) {
    return registerAsync(server, context, ServiceLoader.load(ComponentProvider.class));
  }

  static boolean registerAsync(
      McpAsyncServer server, McpApplicationContext context, Iterable<ComponentProvider> providers) {
    List<ResourceDefinition> definitions = loadDefinitions(providers, context);
    if (definitions.isEmpty()) {
      return false;
    }
    rejectDuplicateNames(definitions);
    for (ResourceDefinition definition : definitions) {
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
      log.debug("Async McpResource {} registered successfully", definition.resource().name());
    }
    return true;
  }

  private static List<ResourceDefinition> loadDefinitions(
      Iterable<ComponentProvider> providers, McpApplicationContext context) {
    List<ResourceDefinition> definitions = new ArrayList<>();
    for (ComponentProvider provider : providers) {
      for (ResourceDefinition definition : provider.resources()) {
        if (context.isInScope(definition.sourceMethod())) {
          definitions.add(definition);
        }
      }
    }
    return definitions;
  }

  private static void rejectDuplicateNames(List<ResourceDefinition> definitions) {
    Map<String, String> registeredNames = new HashMap<>();
    for (ResourceDefinition definition : definitions) {
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
    final String text = invocation.asText();
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
