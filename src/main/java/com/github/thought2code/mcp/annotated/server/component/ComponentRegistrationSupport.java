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

/**
 * Shared helpers for loading, validating, and registering build-time MCP component definitions.
 *
 * <p>Registration classes ({@link
 * com.github.thought2code.mcp.annotated.server.component.tool.ToolRegistration}, {@link
 * com.github.thought2code.mcp.annotated.server.component.prompt.PromptRegistration}, and related
 * types) delegate scope filtering, duplicate detection, and sync/async registration loops to this
 * utility so behavior stays consistent across component kinds.
 *
 * @author codeboyzhou
 */
public final class ComponentRegistrationSupport {

  private ComponentRegistrationSupport() {}

  /** Builds a duplicate-name error message from the colliding MCP name and source methods. */
  @FunctionalInterface
  public interface DuplicateMessageBuilder {
    /**
     * @param name MCP-visible component name that collided
     * @param previousMethod fully qualified source method that registered first
     * @param currentMethod fully qualified source method that collided
     * @return human-readable error message
     */
    String build(String name, String previousMethod, String currentMethod);
  }

  /**
   * Builds a duplicate-key error message when the collision key is derived from the full definition
   * (for example completion references).
   *
   * @param <D> definition type
   */
  @FunctionalInterface
  public interface DuplicateDefinitionMessageBuilder<D> {
    /**
     * @param definition definition that failed duplicate validation
     * @param previousMethod fully qualified source method that registered first
     * @param currentMethod fully qualified source method that collided
     * @return human-readable error message
     */
    String build(D definition, String previousMethod, String currentMethod);
  }

  /**
   * Collects in-scope component definitions from all {@link ComponentProvider} instances.
   *
   * @param <D> definition type
   * @param providers SPI providers discovered at runtime
   * @param context application context used for base-package scope filtering
   * @param providerExtractor extracts definitions from one provider
   * @param sourceMethodExtractor resolves the diagnostic source-method id on a definition
   * @return definitions whose source methods are in scope; not deduplicated
   */
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

  /**
   * Loads in-scope definitions and rejects duplicate MCP-visible names before registration.
   *
   * @param <D> definition type
   * @param providers SPI providers discovered at runtime
   * @param context application context used for scope filtering
   * @param providerExtractor extracts definitions from one provider
   * @param sourceMethodExtractor resolves the diagnostic source-method id
   * @param nameExtractor resolves the MCP-visible name used for duplicate detection
   * @param messageBuilder builds the error message when a duplicate name is found
   * @return validated definitions ready for registration
   * @throws com.github.thought2code.mcp.annotated.exception.McpServerComponentRegistrationException
   *     when duplicate names are detected
   */
  public static <D> List<D> prepareDefinitions(
      Iterable<ComponentProvider> providers,
      McpApplicationContext context,
      Function<ComponentProvider, List<D>> providerExtractor,
      Function<D, String> sourceMethodExtractor,
      Function<D, String> nameExtractor,
      DuplicateMessageBuilder messageBuilder) {
    List<D> definitions =
        loadDefinitions(providers, context, providerExtractor, sourceMethodExtractor);
    rejectDuplicateNames(definitions, nameExtractor, sourceMethodExtractor, messageBuilder);
    return definitions;
  }

  /**
   * Rejects definitions that share the same MCP-visible name.
   *
   * @param <D> definition type
   * @param definitions definitions to validate
   * @param nameExtractor resolves the MCP-visible name
   * @param sourceMethodExtractor resolves the diagnostic source-method id
   * @param messageBuilder builds the error message when a duplicate name is found
   * @throws com.github.thought2code.mcp.annotated.exception.McpServerComponentRegistrationException
   *     when duplicate names are detected
   */
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

  /**
   * Rejects definitions that share the same registration key.
   *
   * @param <D> definition type
   * @param definitions definitions to validate
   * @param keyExtractor resolves the deduplication key (name, completion reference, etc.)
   * @param sourceMethodExtractor resolves the diagnostic source-method id
   * @param messageBuilder builds the error message when a duplicate key is found
   * @throws com.github.thought2code.mcp.annotated.exception.McpServerComponentRegistrationException
   *     when duplicate keys are detected
   */
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

  /**
   * Blocks on one async MCP registration and wraps failures as registration exceptions.
   *
   * @param registration reactive registration to complete
   * @param componentTypeName component kind label used in error messages (for example {@code
   *     McpTool})
   * @param specificationName MCP specification name being registered
   * @param log logger for error reporting
   * @throws com.github.thought2code.mcp.annotated.exception.McpServerComponentRegistrationException
   *     when registration fails
   */
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

  /**
   * Registers all definitions on a sync MCP server.
   *
   * @param <D> definition type
   * @param definitions definitions to register; may be empty
   * @param registerAction registers one definition on the sync server
   * @param onRegistered callback invoked after each successful registration (typically logging)
   * @return {@code true} when at least one definition was registered
   */
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

  /**
   * Registers all definitions on an async MCP server, blocking per registration.
   *
   * @param <D> definition type
   * @param definitions definitions to register; may be empty
   * @param registerAction registers one definition on the async server
   * @param specificationNameExtractor resolves the MCP specification name for error messages
   * @param componentTypeName component kind label used in error messages
   * @param log logger for error reporting
   * @param onRegistered callback invoked after each successful registration
   * @return {@code true} when at least one definition was registered
   * @throws com.github.thought2code.mcp.annotated.exception.McpServerComponentRegistrationException
   *     when any async registration fails
   */
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
