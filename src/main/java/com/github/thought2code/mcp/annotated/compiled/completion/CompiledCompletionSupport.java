package com.github.thought2code.mcp.annotated.compiled.completion;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.compiled.spi.McpCompiledModelProvider;
import com.github.thought2code.mcp.annotated.exception.McpServerComponentRegistrationException;
import com.github.thought2code.mcp.annotated.server.component.McpCompleteCompletion;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import reactor.core.publisher.Mono;

/** Builds completion specifications from build-time compiled completion models. */
public final class CompiledCompletionSupport {

  private CompiledCompletionSupport() {}

  public static List<McpServerFeatures.SyncCompletionSpecification> allSync(
      McpApplicationContext context) {
    return allSync(context, ServiceLoader.load(McpCompiledModelProvider.class));
  }

  static List<McpServerFeatures.SyncCompletionSpecification> allSync(
      McpApplicationContext context, Iterable<McpCompiledModelProvider> providers) {
    List<CompiledCompletionDefinition> definitions = loadDefinitions(providers, context);
    List<McpServerFeatures.SyncCompletionSpecification> completions = new ArrayList<>();
    for (CompiledCompletionDefinition definition : definitions) {
      completions.add(
          new McpServerFeatures.SyncCompletionSpecification(
              definition.reference(), (exchange, request) -> invoke(definition, context, request)));
    }
    return completions;
  }

  public static List<McpServerFeatures.AsyncCompletionSpecification> allAsync(
      McpApplicationContext context) {
    return allAsync(context, ServiceLoader.load(McpCompiledModelProvider.class));
  }

  static List<McpServerFeatures.AsyncCompletionSpecification> allAsync(
      McpApplicationContext context, Iterable<McpCompiledModelProvider> providers) {
    List<CompiledCompletionDefinition> definitions = loadDefinitions(providers, context);
    List<McpServerFeatures.AsyncCompletionSpecification> completions = new ArrayList<>();
    for (CompiledCompletionDefinition definition : definitions) {
      completions.add(
          new McpServerFeatures.AsyncCompletionSpecification(
              definition.reference(),
              (exchange, request) ->
                  Mono.fromCallable(() -> invoke(definition, context, request))));
    }
    return completions;
  }

  private static List<CompiledCompletionDefinition> loadDefinitions(
      Iterable<McpCompiledModelProvider> providers, McpApplicationContext context) {
    List<CompiledCompletionDefinition> definitions = new ArrayList<>();
    for (McpCompiledModelProvider provider : providers) {
      for (CompiledCompletionDefinition definition : provider.completions()) {
        if (context.isInScope(definition.sourceMethod())) {
          definitions.add(definition);
        }
      }
    }
    return definitions;
  }

  private static McpSchema.CompleteResult invoke(
      CompiledCompletionDefinition definition,
      McpApplicationContext context,
      McpSchema.CompleteRequest request) {
    var invocation = definition.invoker().invoke(context, request.argument());
    if (invocation.isError()) {
      throw new McpServerComponentRegistrationException(
          "Completion invocation failed for " + definition.sourceMethod());
    }
    Object raw = invocation.result();
    if (!(raw instanceof McpCompleteCompletion completion)) {
      throw new McpServerComponentRegistrationException(
          "Completion method must return McpCompleteCompletion: " + definition.sourceMethod());
    }
    return new McpSchema.CompleteResult(
        new McpSchema.CompleteResult.CompleteCompletion(
            completion.values(), completion.total(), completion.hasMore()));
  }
}
