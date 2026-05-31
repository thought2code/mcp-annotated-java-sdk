package com.github.thought2code.mcp.annotated.compiled.prompt;

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
import com.github.thought2code.mcp.annotated.compiled.spi.McpCompiledModelProvider;
import com.github.thought2code.mcp.annotated.exception.McpServerComponentRegistrationException;
import com.github.thought2code.mcp.annotated.reflect.Invocation;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class CompiledPromptRegistrationTest {

  @Test
  void registerSync_shouldReturnFalseWhenNoCompiledProvider() {
    McpSyncServer server = mock(McpSyncServer.class);
    McpApplicationContext context = mock(McpApplicationContext.class);

    boolean registered = CompiledPromptRegistration.registerSync(server, context, List.of());

    assertFalse(registered);
    verify(server, times(0)).addPrompt(any());
  }

  @Test
  void registerSync_shouldRegisterCompiledPrompt() {
    McpSyncServer server = mock(McpSyncServer.class);
    McpApplicationContext context = mock(McpApplicationContext.class);
    McpCompiledModelProvider provider =
        new McpCompiledModelProvider() {
          @Override
          public List<CompiledPromptDefinition> prompts() {
            McpSchema.Prompt prompt = McpSchema.Prompt.builder("compiled_prompt").build();
            return List.of(
                new CompiledPromptDefinition(
                    "test.Source#prompt()",
                    prompt,
                    "compiled_prompt",
                    (ctx, args) -> Invocation.builder().result("ok").build()));
          }
        };
    when(context.isInScope(anyString())).thenReturn(true);

    boolean registered =
        CompiledPromptRegistration.registerSync(server, context, List.of(provider));

    assertTrue(registered);
    verify(server, times(1)).addPrompt(any());
  }

  @Test
  void registerSync_shouldRejectDuplicatePromptNames() {
    McpSyncServer server = mock(McpSyncServer.class);
    McpApplicationContext context = mock(McpApplicationContext.class);
    CompiledPromptDefinition a =
        new CompiledPromptDefinition(
            "test.Source#a()",
            McpSchema.Prompt.builder("duplicate").build(),
            "duplicate",
            (ctx, args) -> Invocation.builder().result("a").build());
    CompiledPromptDefinition b =
        new CompiledPromptDefinition(
            "test.Source#b()",
            McpSchema.Prompt.builder("duplicate").build(),
            "duplicate",
            (ctx, args) -> Invocation.builder().result("b").build());
    McpCompiledModelProvider provider =
        new McpCompiledModelProvider() {
          @Override
          public List<CompiledPromptDefinition> prompts() {
            return List.of(a, b);
          }
        };
    when(context.isInScope(anyString())).thenReturn(true);

    assertThrows(
        McpServerComponentRegistrationException.class,
        () -> CompiledPromptRegistration.registerSync(server, context, List.of(provider)));
  }

  @Test
  void registerAsync_shouldRegisterCompiledPrompt() {
    McpAsyncServer server = mock(McpAsyncServer.class);
    when(server.addPrompt(any())).thenReturn(Mono.empty());
    McpApplicationContext context = mock(McpApplicationContext.class);
    McpCompiledModelProvider provider =
        new McpCompiledModelProvider() {
          @Override
          public List<CompiledPromptDefinition> prompts() {
            McpSchema.Prompt prompt = McpSchema.Prompt.builder("compiled_prompt_async").build();
            return List.of(
                new CompiledPromptDefinition(
                    "test.Source#prompt()",
                    prompt,
                    "compiled_prompt_async",
                    (ctx, args) -> Invocation.builder().result("ok").build()));
          }
        };
    when(context.isInScope(anyString())).thenReturn(true);

    boolean registered =
        CompiledPromptRegistration.registerAsync(server, context, List.of(provider));

    assertTrue(registered);
    verify(server, times(1)).addPrompt(any());
  }
}
