package com.github.thought2code.mcp.annotated.server.component;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.exception.McpServerComponentRegistrationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import org.slf4j.Logger;
import reactor.core.publisher.Mono;

/** Shared skeleton helpers for component registration workflows. */
public final class ComponentRegistrationSupport {

  private ComponentRegistrationSupport() {}

  @FunctionalInterface
  public interface DuplicateMessageBuilder {
    String build(String name, String previousMethod, String currentMethod);
  }

  @FunctionalInterface
  public interface DuplicateDefinitionMessageBuilder<D> {
    String build(D definition, String previousMethod, String currentMethod);
  }

  public static <D> List<D> loadDefinitions(
      Iterable<ComponentProvider> providers,
      McpApplicationContext context,
      Function<ComponentProvider, List<D>> providerExtractor,
      Function<D, String> sourceMethodExtractor) {
    List<D> definitions = new ArrayList<>();
    for (ComponentProvider provider : providers) {
      for (D definition : providerExtractor.apply(provider)) {
        if (context.isInScope(sourceMethodExtractor.apply(definition))) {
          definitions.add(definition);
        }
      }
    }
    return definitions;
  }

  public static <D> List<D> prepareDefinitions(
      Iterable<ComponentProvider> providers,
      McpApplicationContext context,
      Function<ComponentProvider, List<D>> providerExtractor,
      Function<D, String> sourceMethodExtractor,
      Function<D, String> nameExtractor,
      DuplicateMessageBuilder messageBuilder) {
    List<D> definitions = loadDefinitions(providers, context, providerExtractor, sourceMethodExtractor);
    rejectDuplicateNames(definitions, nameExtractor, sourceMethodExtractor, messageBuilder);
    return definitions;
  }

  public static <D> void rejectDuplicateNames(
      List<D> definitions,
      Function<D, String> nameExtractor,
      Function<D, String> sourceMethodExtractor,
      DuplicateMessageBuilder messageBuilder) {
    rejectDuplicateDefinitions(
        definitions,
        nameExtractor,
        sourceMethodExtractor,
        (definition, previousMethod, currentMethod) ->
            messageBuilder.build(nameExtractor.apply(definition), previousMethod, currentMethod));
  }

  public static <D> void rejectDuplicateDefinitions(
      List<D> definitions,
      Function<D, String> keyExtractor,
      Function<D, String> sourceMethodExtractor,
      DuplicateDefinitionMessageBuilder<D> messageBuilder) {
    Map<String, String> registeredNames = new HashMap<>();
    for (D definition : definitions) {
      String name = keyExtractor.apply(definition);
      String sourceMethod = sourceMethodExtractor.apply(definition);
      String previous = registeredNames.putIfAbsent(name, sourceMethod);
      if (previous != null) {
        throw new McpServerComponentRegistrationException(
            messageBuilder.build(definition, previous, sourceMethod));
      }
    }
  }

  public static void awaitAsyncRegistration(
      Mono<Void> registration, String componentTypeName, String specificationName, Logger log) {
    try {
      registration.block();
    } catch (RuntimeException e) {
      String message =
          String.format("Failed to register async %s %s", componentTypeName, specificationName);
      log.error(message, e);
      throw new McpServerComponentRegistrationException(message, e);
    }
  }

  public static <D> boolean registerSyncDefinitions(
      List<D> definitions, Consumer<D> registerAction, Consumer<D> onRegistered) {
    if (definitions.isEmpty()) {
      return false;
    }
    for (D definition : definitions) {
      registerAction.accept(definition);
      onRegistered.accept(definition);
    }
    return true;
  }

  public static <D> boolean registerAsyncDefinitions(
      List<D> definitions,
      Function<D, Mono<Void>> registerAction,
      Function<D, String> specificationNameExtractor,
      String componentTypeName,
      Logger log,
      Consumer<D> onRegistered) {
    if (definitions.isEmpty()) {
      return false;
    }
    for (D definition : definitions) {
      awaitAsyncRegistration(
          registerAction.apply(definition),
          componentTypeName,
          specificationNameExtractor.apply(definition),
          log);
      onRegistered.accept(definition);
    }
    return true;
  }
}
