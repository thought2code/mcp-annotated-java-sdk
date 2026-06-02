package com.github.thought2code.mcp.annotated.server.component.completion;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.exception.McpServerComponentRegistrationException;
import com.github.thought2code.mcp.annotated.server.component.ComponentProvider;
import com.github.thought2code.mcp.annotated.server.component.ComponentRegistrationSupport;
import com.github.thought2code.mcp.annotated.server.component.DuplicateComponentMessageHelper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import reactor.core.publisher.Mono;

/**
 * Builds MCP completion specifications from build-time completion definitions.
 *
 * <p>Unlike tools, prompts, and resources, completions are returned as specification lists consumed
 * when constructing the MCP server rather than registered incrementally after startup.
 *
 * @author codeboyzhou
 */
public final class CompletionSupport {

  private CompletionSupport() {}

  /**
   * Returns sync completion specifications for all in-scope completion definitions.
   *
   * @param context application context
   * @return sync completion specifications; may be empty
   */
  public static List<McpServerFeatures.SyncCompletionSpecification> allSync(
      McpApplicationContext context) {
    return allSync(context, ServiceLoader.load(ComponentProvider.class));
  }

  /** Returns sync completions using an explicit provider iterable (for tests). */
  static List<McpServerFeatures.SyncCompletionSpecification> allSync(
      McpApplicationContext context, Iterable<ComponentProvider> providers) {
    List<CompletionDefinition> definitions =
        ComponentRegistrationSupport.loadDefinitions(
            providers, context, ComponentProvider::completions, CompletionDefinition::sourceMethod);
    rejectDuplicateReferences(definitions);
    List<McpServerFeatures.SyncCompletionSpecification> completions = new ArrayList<>();
    for (CompletionDefinition definition : definitions) {
      completions.add(
          new McpServerFeatures.SyncCompletionSpecification(
              definition.reference(), (exchange, request) -> invoke(definition, context, request)));
    }
    return completions;
  }

  /**
   * Returns async completion specifications for all in-scope completion definitions.
   *
   * @param context application context
   * @return async completion specifications; may be empty
   */
  public static List<McpServerFeatures.AsyncCompletionSpecification> allAsync(
      McpApplicationContext context) {
    return allAsync(context, ServiceLoader.load(ComponentProvider.class));
  }

  /** Returns async completions using an explicit provider iterable (for tests). */
  static List<McpServerFeatures.AsyncCompletionSpecification> allAsync(
      McpApplicationContext context, Iterable<ComponentProvider> providers) {
    List<CompletionDefinition> definitions =
        ComponentRegistrationSupport.loadDefinitions(
            providers, context, ComponentProvider::completions, CompletionDefinition::sourceMethod);
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

  /** Rejects completion definitions that target the same prompt or resource reference. */
  private static void rejectDuplicateReferences(List<CompletionDefinition> definitions) {
    ComponentRegistrationSupport.rejectDuplicateDefinitions(
        definitions,
        definition -> completionReferenceKey(definition.reference()),
        CompletionDefinition::sourceMethod,
        (definition, previousMethod, currentMethod) ->
            DuplicateComponentMessageHelper.duplicateCompletionReference(
                DuplicateComponentMessageHelper.completionReferenceDescription(
                    definition.reference()),
                previousMethod,
                currentMethod));
  }

  /** Stable deduplication key for a completion reference. */
  private static String completionReferenceKey(McpSchema.CompleteReference reference) {
    if (reference instanceof McpSchema.ResourceReference resourceReference) {
      return "resource:" + resourceReference.uri();
    }
    if (reference instanceof McpSchema.PromptReference promptReference) {
      return "prompt:" + promptReference.name();
    }
    return reference == null ? "unknown:null" : "unknown:" + reference;
  }

  /**
   * Invokes a completion handler and maps a {@link CompletionResult} to MCP wire format.
   *
   * @throws McpServerComponentRegistrationException when invocation fails or return type is invalid
   */
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
