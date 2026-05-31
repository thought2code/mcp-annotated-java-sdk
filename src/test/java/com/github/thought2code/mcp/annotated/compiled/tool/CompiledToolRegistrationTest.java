package com.github.thought2code.mcp.annotated.compiled.tool;

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
import java.util.Map;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class CompiledToolRegistrationTest {

  @Test
  void registerSync_shouldReturnFalseWhenNoCompiledProvider() {
    McpSyncServer server = mock(McpSyncServer.class);
    McpApplicationContext context = mock(McpApplicationContext.class);

    boolean registered = CompiledToolRegistration.registerSync(server, context, List.of());

    assertFalse(registered);
    verify(server, times(0)).addTool(any());
  }

  @Test
  void registerSync_shouldRegisterCompiledTool() {
    McpSyncServer server = mock(McpSyncServer.class);
    McpApplicationContext context = mock(McpApplicationContext.class);
    McpCompiledModelProvider provider =
        new McpCompiledModelProvider() {
          @Override
          public List<CompiledToolDefinition> tools() {
            return List.of(
                new CompiledToolDefinition(
                    "test.Source#tool()",
                    McpSchema.Tool.builder("compiled_tool", Map.of()).build(),
                    (ctx, args) -> Invocation.builder().result("ok").build()));
          }
        };
    when(context.isInScope(anyString())).thenReturn(true);

    boolean registered = CompiledToolRegistration.registerSync(server, context, List.of(provider));

    assertTrue(registered);
    verify(server, times(1)).addTool(any());
  }

  @Test
  void registerSync_shouldRejectDuplicateToolNames() {
    McpSyncServer server = mock(McpSyncServer.class);
    McpApplicationContext context = mock(McpApplicationContext.class);
    CompiledToolDefinition a =
        new CompiledToolDefinition(
            "test.Source#a()",
            McpSchema.Tool.builder("duplicate", Map.of()).build(),
            (ctx, args) -> Invocation.builder().result("a").build());
    CompiledToolDefinition b =
        new CompiledToolDefinition(
            "test.Source#b()",
            McpSchema.Tool.builder("duplicate", Map.of()).build(),
            (ctx, args) -> Invocation.builder().result("b").build());
    McpCompiledModelProvider provider =
        new McpCompiledModelProvider() {
          @Override
          public List<CompiledToolDefinition> tools() {
            return List.of(a, b);
          }
        };
    when(context.isInScope(anyString())).thenReturn(true);

    assertThrows(
        McpServerComponentRegistrationException.class,
        () -> CompiledToolRegistration.registerSync(server, context, List.of(provider)));
  }

  @Test
  void registerAsync_shouldRegisterCompiledTool() {
    McpAsyncServer server = mock(McpAsyncServer.class);
    when(server.addTool(any())).thenReturn(Mono.empty());
    McpApplicationContext context = mock(McpApplicationContext.class);
    McpCompiledModelProvider provider =
        new McpCompiledModelProvider() {
          @Override
          public List<CompiledToolDefinition> tools() {
            return List.of(
                new CompiledToolDefinition(
                    "test.Source#tool()",
                    McpSchema.Tool.builder("compiled_tool_async", Map.of()).build(),
                    (ctx, args) -> Invocation.builder().result("ok").build()));
          }
        };
    when(context.isInScope(anyString())).thenReturn(true);

    boolean registered = CompiledToolRegistration.registerAsync(server, context, List.of(provider));

    assertTrue(registered);
    verify(server, times(1)).addTool(any());
  }
}
