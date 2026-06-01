package com.github.thought2code.mcp.annotated.server.component.prompt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.exception.McpServerComponentRegistrationException;
import com.github.thought2code.mcp.annotated.server.component.ComponentProvider;
import com.github.thought2code.mcp.annotated.server.component.Invocation;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class PromptRegistrationTest {

  @Test
  void registerSync_shouldReturnFalseWhenNoComponentProvider() {
    McpSyncServer server = mock(McpSyncServer.class);
    McpApplicationContext context = mock(McpApplicationContext.class);

    boolean registered = PromptRegistration.registerSync(server, context, List.of());

    assertFalse(registered);
    verify(server, times(0)).addPrompt(any());
  }

  @Test
  void registerSync_shouldRegisterPrompt() {
    McpSyncServer server = mock(McpSyncServer.class);
    McpApplicationContext context = mock(McpApplicationContext.class);
    ComponentProvider provider =
        new ComponentProvider() {
          @Override
          public List<PromptDefinition> prompts() {
            McpSchema.Prompt prompt = McpSchema.Prompt.builder("component_prompt").build();
            return List.of(
                new PromptDefinition(
                    "test.Source#prompt()",
                    prompt,
                    "component_prompt",
                    (ctx, args) -> Invocation.builder().result("ok").build()));
          }
        };
    when(context.isInScope(anyString())).thenReturn(true);

    boolean registered = PromptRegistration.registerSync(server, context, List.of(provider));

    assertTrue(registered);
    verify(server, times(1)).addPrompt(any());
  }

  @Test
  void registerSync_shouldRejectDuplicatePromptNames() {
    McpSyncServer server = mock(McpSyncServer.class);
    McpApplicationContext context = mock(McpApplicationContext.class);
    PromptDefinition a =
        new PromptDefinition(
            "test.Source#a()",
            McpSchema.Prompt.builder("duplicate").build(),
            "duplicate",
            (ctx, args) -> Invocation.builder().result("a").build());
    PromptDefinition b =
        new PromptDefinition(
            "test.Source#b()",
            McpSchema.Prompt.builder("duplicate").build(),
            "duplicate",
            (ctx, args) -> Invocation.builder().result("b").build());
    ComponentProvider provider =
        new ComponentProvider() {
          @Override
          public List<PromptDefinition> prompts() {
            return List.of(a, b);
          }
        };
    when(context.isInScope(anyString())).thenReturn(true);

    McpServerComponentRegistrationException exception =
        assertThrows(
            McpServerComponentRegistrationException.class,
            () -> PromptRegistration.registerSync(server, context, List.of(provider)));
    assertTrue(exception.getMessage().contains("Duplicate prompt name 'duplicate'"));
  }

  @Test
  void registerAsync_shouldRegisterPrompt() {
    McpAsyncServer server = mock(McpAsyncServer.class);
    when(server.addPrompt(any())).thenReturn(Mono.empty());
    McpApplicationContext context = mock(McpApplicationContext.class);
    ComponentProvider provider =
        new ComponentProvider() {
          @Override
          public List<PromptDefinition> prompts() {
            McpSchema.Prompt prompt = McpSchema.Prompt.builder("component_prompt_async").build();
            return List.of(
                new PromptDefinition(
                    "test.Source#prompt()",
                    prompt,
                    "component_prompt_async",
                    (ctx, args) -> Invocation.builder().result("ok").build()));
          }
        };
    when(context.isInScope(anyString())).thenReturn(true);

    boolean registered = PromptRegistration.registerAsync(server, context, List.of(provider));

    assertTrue(registered);
    verify(server, times(1)).addPrompt(any());
  }
}
