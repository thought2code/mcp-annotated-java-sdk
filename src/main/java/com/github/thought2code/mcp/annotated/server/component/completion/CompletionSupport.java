package com.github.thought2code.mcp.annotated.server.component.completion;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.exception.McpServerComponentRegistrationException;
import com.github.thought2code.mcp.annotated.server.component.ComponentProvider;
import com.github.thought2code.mcp.annotated.server.component.DuplicateComponentMessageHelper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import reactor.core.publisher.Mono;

/** Builds completion specifications from build-time component completion models. */
public final class CompletionSupport {

  private CompletionSupport() {}

  public static List<McpServerFeatures.SyncCompletionSpecification> allSync(
      McpApplicationContext context) {
    return allSync(context, ServiceLoader.load(ComponentProvider.class));
  }

  static List<McpServerFeatures.SyncCompletionSpecification> allSync(
      McpApplicationContext context, Iterable<ComponentProvider> providers) {
    List<CompletionDefinition> definitions = loadDefinitions(providers, context);
    rejectDuplicateReferences(definitions);
    List<McpServerFeatures.SyncCompletionSpecification> completions = new ArrayList<>();
    for (CompletionDefinition definition : definitions) {
      completions.add(
          new McpServerFeatures.SyncCompletionSpecification(
              definition.reference(), (exchange, request) -> invoke(definition, context, request)));
    }
    return completions;
  }

  public static List<McpServerFeatures.AsyncCompletionSpecification> allAsync(
      McpApplicationContext context) {
    return allAsync(context, ServiceLoader.load(ComponentProvider.class));
  }

  static List<McpServerFeatures.AsyncCompletionSpecification> allAsync(
      McpApplicationContext context, Iterable<ComponentProvider> providers) {
    List<CompletionDefinition> definitions = loadDefinitions(providers, context);
    rejectDuplicateReferences(definitions);
    List<McpServerFeatures.AsyncCompletionSpecification> completions = new ArrayList<>();
    for (CompletionDefinition definition : definitions) {
      completions.add(
          new McpServerFeatures.AsyncCompletionSpecification(
              definition.reference(),
              (exchange, request) ->
                  Mono.fromCallable(() -> invoke(definition, context, request))));
    }
    return completions;
  }

  private static List<CompletionDefinition> loadDefinitions(
      Iterable<ComponentProvider> providers, McpApplicationContext context) {
    List<CompletionDefinition> definitions = new ArrayList<>();
    for (ComponentProvider provider : providers) {
      for (CompletionDefinition definition : provider.completions()) {
        if (context.isInScope(definition.sourceMethod())) {
          definitions.add(definition);
        }
      }
    }
    return definitions;
  }

  private static void rejectDuplicateReferences(List<CompletionDefinition> definitions) {
    Map<String, String> registeredReferences = new HashMap<>();
    for (CompletionDefinition definition : definitions) {
      String referenceKey = completionReferenceKey(definition.reference());
      String previous = registeredReferences.putIfAbsent(referenceKey, definition.sourceMethod());
      if (previous != null) {
        throw new McpServerComponentRegistrationException(
            DuplicateComponentMessageHelper.duplicateCompletionReference(
                DuplicateComponentMessageHelper.completionReferenceDescription(
                    definition.reference()),
                previous,
                definition.sourceMethod()));
      }
    }
  }

  private static String completionReferenceKey(McpSchema.CompleteReference reference) {
    if (reference instanceof McpSchema.ResourceReference resourceReference) {
      return "resource:" + resourceReference.uri();
    }
    if (reference instanceof McpSchema.PromptReference promptReference) {
      return "prompt:" + promptReference.name();
    }
    return reference == null ? "unknown:null" : "unknown:" + reference;
  }

  private static McpSchema.CompleteResult invoke(
      CompletionDefinition definition,
      McpApplicationContext context,
      McpSchema.CompleteRequest request) {
    var invocation = definition.invoker().invoke(context, request.argument());
    if (invocation.isError()) {
      throw new McpServerComponentRegistrationException(
          "Completion invocation failed for " + definition.sourceMethod());
    }
    Object raw = invocation.result();
    if (!(raw instanceof CompletionResult completion)) {
      throw new McpServerComponentRegistrationException(
          "Completion method must return CompletionResult: " + definition.sourceMethod());
    }
    return new McpSchema.CompleteResult(
        new McpSchema.CompleteResult.CompleteCompletion(
            completion.values(), completion.total(), completion.hasMore()));
  }
}
