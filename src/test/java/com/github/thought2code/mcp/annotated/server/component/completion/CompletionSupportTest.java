package com.github.thought2code.mcp.annotated.server.component.completion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.server.component.ComponentProvider;
import com.github.thought2code.mcp.annotated.server.component.Invocation;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompletionSupportTest {

  @Test
  void allSync_shouldReturnEmptyWhenNoComponentProvider() {
    McpApplicationContext context = mock(McpApplicationContext.class);
    List<McpServerFeatures.SyncCompletionSpecification> specs =
        CompletionSupport.allSync(context, List.of());
    assertTrue(specs.isEmpty());
  }

  @Test
  void allSync_shouldBuildCompletionSpecification() {
    McpApplicationContext context = mock(McpApplicationContext.class);
    when(context.isInScope(anyString())).thenReturn(true);
    ComponentProvider provider =
        new ComponentProvider() {
          @Override
          public List<CompletionDefinition> completions() {
            return List.of(
                new CompletionDefinition(
                    "test.Source#completion()",
                    McpSchema.PromptReference.builder("prompt_name").build(),
                    (ctx, argument) ->
                        Invocation.builder()
                            .result(new CompletionResult(List.of("a", "b"), 2, false))
                            .build()));
          }
        };

    List<McpServerFeatures.SyncCompletionSpecification> specs =
        CompletionSupport.allSync(context, List.of(provider));

    assertEquals(1, specs.size());
    McpSchema.CompleteRequest request =
        new McpSchema.CompleteRequest(
            McpSchema.PromptReference.builder("prompt_name").build(),
            new McpSchema.CompleteRequest.CompleteArgument("arg", "v"));
    McpSchema.CompleteResult result = specs.get(0).completionHandler().apply(null, request);
    assertNotNull(result);
    assertEquals(List.of("a", "b"), result.completion().values());
  }

  @Test
  void allAsync_shouldBuildCompletionSpecification() {
    McpApplicationContext context = mock(McpApplicationContext.class);
    when(context.isInScope(anyString())).thenReturn(true);
    ComponentProvider provider =
        new ComponentProvider() {
          @Override
          public List<CompletionDefinition> completions() {
            return List.of(
                new CompletionDefinition(
                    "test.Source#completion()",
                    new McpSchema.ResourceReference("resource://test"),
                    (ctx, argument) ->
                        Invocation.builder()
                            .result(new CompletionResult(List.of("one"), 1, false))
                            .build()));
          }
        };

    List<McpServerFeatures.AsyncCompletionSpecification> specs =
        CompletionSupport.allAsync(context, List.of(provider));

    assertEquals(1, specs.size());
    assertFalse(specs.isEmpty());
  }
}
