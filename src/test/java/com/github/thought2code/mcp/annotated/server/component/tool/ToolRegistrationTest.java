package com.github.thought2code.mcp.annotated.server.component.tool;

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
import com.github.thought2code.mcp.annotated.reflect.Invocation;
import com.github.thought2code.mcp.annotated.server.component.spi.ComponentModelProvider;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class ToolRegistrationTest {

  @Test
  void registerSync_shouldReturnFalseWhenNoComponentProvider() {
    McpSyncServer server = mock(McpSyncServer.class);
    McpApplicationContext context = mock(McpApplicationContext.class);

    boolean registered = ToolRegistration.registerSync(server, context, List.of());

    assertFalse(registered);
    verify(server, times(0)).addTool(any());
  }

  @Test
  void registerSync_shouldRegisterTool() {
    McpSyncServer server = mock(McpSyncServer.class);
    McpApplicationContext context = mock(McpApplicationContext.class);
    ComponentModelProvider provider =
        new ComponentModelProvider() {
          @Override
          public List<ToolDefinition> tools() {
            return List.of(
                new ToolDefinition(
                    "test.Source#tool()",
                    McpSchema.Tool.builder("component_tool", Map.of()).build(),
                    (ctx, args) -> Invocation.builder().result("ok").build()));
          }
        };
    when(context.isInScope(anyString())).thenReturn(true);

    boolean registered = ToolRegistration.registerSync(server, context, List.of(provider));

    assertTrue(registered);
    verify(server, times(1)).addTool(any());
  }

  @Test
  void registerSync_shouldRejectDuplicateToolNames() {
    McpSyncServer server = mock(McpSyncServer.class);
    McpApplicationContext context = mock(McpApplicationContext.class);
    ToolDefinition a =
        new ToolDefinition(
            "test.Source#a()",
            McpSchema.Tool.builder("duplicate", Map.of()).build(),
            (ctx, args) -> Invocation.builder().result("a").build());
    ToolDefinition b =
        new ToolDefinition(
            "test.Source#b()",
            McpSchema.Tool.builder("duplicate", Map.of()).build(),
            (ctx, args) -> Invocation.builder().result("b").build());
    ComponentModelProvider provider =
        new ComponentModelProvider() {
          @Override
          public List<ToolDefinition> tools() {
            return List.of(a, b);
          }
        };
    when(context.isInScope(anyString())).thenReturn(true);

    assertThrows(
        McpServerComponentRegistrationException.class,
        () -> ToolRegistration.registerSync(server, context, List.of(provider)));
  }

  @Test
  void registerAsync_shouldRegisterTool() {
    McpAsyncServer server = mock(McpAsyncServer.class);
    when(server.addTool(any())).thenReturn(Mono.empty());
    McpApplicationContext context = mock(McpApplicationContext.class);
    ComponentModelProvider provider =
        new ComponentModelProvider() {
          @Override
          public List<ToolDefinition> tools() {
            return List.of(
                new ToolDefinition(
                    "test.Source#tool()",
                    McpSchema.Tool.builder("component_tool_async", Map.of()).build(),
                    (ctx, args) -> Invocation.builder().result("ok").build()));
          }
        };
    when(context.isInScope(anyString())).thenReturn(true);

    boolean registered = ToolRegistration.registerAsync(server, context, List.of(provider));

    assertTrue(registered);
    verify(server, times(1)).addTool(any());
  }
}
