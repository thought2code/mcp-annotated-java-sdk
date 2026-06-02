package com.github.thought2code.mcp.annotated.server.component.completion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.exception.McpServerComponentRegistrationException;
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

  @Test
  void allSync_shouldRejectDuplicatePromptReferences() {
    McpApplicationContext context = mock(McpApplicationContext.class);
    when(context.isInScope(anyString())).thenReturn(true);
    CompletionDefinition a =
        new CompletionDefinition(
            "test.Source#a()",
            McpSchema.PromptReference.builder("duplicate_prompt").build(),
            (ctx, argument) ->
                Invocation.builder().result(new CompletionResult(List.of("a"), 1, false)).build());
    CompletionDefinition b =
        new CompletionDefinition(
            "test.Source#b()",
            McpSchema.PromptReference.builder("duplicate_prompt").build(),
            (ctx, argument) ->
                Invocation.builder().result(new CompletionResult(List.of("b"), 1, false)).build());
    ComponentProvider provider =
        new ComponentProvider() {
          @Override
          public List<CompletionDefinition> completions() {
            return List.of(a, b);
          }
        };

    McpServerComponentRegistrationException exception =
        assertThrows(
            McpServerComponentRegistrationException.class,
            () -> CompletionSupport.allSync(context, List.of(provider)));
    assertTrue(exception.getMessage().contains("prompt name 'duplicate_prompt'"));
  }

  @Test
  void allAsync_shouldRejectDuplicateResourceReferences() {
    McpApplicationContext context = mock(McpApplicationContext.class);
    when(context.isInScope(anyString())).thenReturn(true);
    CompletionDefinition a =
        new CompletionDefinition(
            "test.Source#a()",
            new McpSchema.ResourceReference("resource://duplicate"),
            (ctx, argument) ->
                Invocation.builder().result(new CompletionResult(List.of("a"), 1, false)).build());
    CompletionDefinition b =
        new CompletionDefinition(
            "test.Source#b()",
            new McpSchema.ResourceReference("resource://duplicate"),
            (ctx, argument) ->
                Invocation.builder().result(new CompletionResult(List.of("b"), 1, false)).build());
    ComponentProvider provider =
        new ComponentProvider() {
          @Override
          public List<CompletionDefinition> completions() {
            return List.of(a, b);
          }
        };

    McpServerComponentRegistrationException exception =
        assertThrows(
            McpServerComponentRegistrationException.class,
            () -> CompletionSupport.allAsync(context, List.of(provider)));
    assertTrue(exception.getMessage().contains("resource uri 'resource://duplicate'"));
  }

  @Test
  void allSync_shouldThrowWhenInvocationReturnsError() {
    McpApplicationContext context = mock(McpApplicationContext.class);
    when(context.isInScope(anyString())).thenReturn(true);
    ComponentProvider provider =
        new ComponentProvider() {
          @Override
          public List<CompletionDefinition> completions() {
            return List.of(
                new CompletionDefinition(
                    "test.Source#error()",
                    McpSchema.PromptReference.builder("prompt_name").build(),
                    (ctx, argument) ->
                        Invocation.builder().result("failed").isError(true).build()));
          }
        };

    List<McpServerFeatures.SyncCompletionSpecification> specs =
        CompletionSupport.allSync(context, List.of(provider));
    McpSchema.CompleteRequest request =
        new McpSchema.CompleteRequest(
            McpSchema.PromptReference.builder("prompt_name").build(),
            new McpSchema.CompleteRequest.CompleteArgument("arg", "v"));

    McpServerComponentRegistrationException exception =
        assertThrows(
            McpServerComponentRegistrationException.class,
            () -> specs.get(0).completionHandler().apply(null, request));
    assertTrue(
        exception.getMessage().contains("Completion invocation failed for test.Source#error()"));
  }

  @Test
  void allSync_shouldThrowWhenInvocationReturnsNonCompletionResult() {
    McpApplicationContext context = mock(McpApplicationContext.class);
    when(context.isInScope(anyString())).thenReturn(true);
    ComponentProvider provider =
        new ComponentProvider() {
          @Override
          public List<CompletionDefinition> completions() {
            return List.of(
                new CompletionDefinition(
                    "test.Source#wrongReturn()",
                    McpSchema.PromptReference.builder("prompt_name").build(),
                    (ctx, argument) -> Invocation.builder().result("not-a-completion").build()));
          }
        };

    List<McpServerFeatures.SyncCompletionSpecification> specs =
        CompletionSupport.allSync(context, List.of(provider));
    McpSchema.CompleteRequest request =
        new McpSchema.CompleteRequest(
            McpSchema.PromptReference.builder("prompt_name").build(),
            new McpSchema.CompleteRequest.CompleteArgument("arg", "v"));

    McpServerComponentRegistrationException exception =
        assertThrows(
            McpServerComponentRegistrationException.class,
            () -> specs.get(0).completionHandler().apply(null, request));
    assertTrue(
        exception
            .getMessage()
            .contains("Completion method must return CompletionResult: test.Source#wrongReturn()"));
  }
}
