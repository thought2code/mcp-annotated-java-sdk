package com.github.thought2code.mcp.annotated.server.component;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.annotation.McpTool;
import com.github.thought2code.mcp.annotated.exception.McpServerComponentRegistrationException;
import com.github.thought2code.mcp.annotated.support.DuplicateToolComponents;
import com.github.thought2code.mcp.annotated.test.TestMcpTools;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpSyncServer;
import java.lang.reflect.Method;
import java.util.Set;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class McpServerToolRegistrationTest {

  @Test
  void register_shouldRejectDuplicateToolNames() throws Exception {
    Method methodA = DuplicateToolComponents.class.getDeclaredMethod("toolA");
    Method methodB = DuplicateToolComponents.class.getDeclaredMethod("toolB");

    McpApplicationContext context = mock(McpApplicationContext.class);
    when(context.getMethodsAnnotatedWith(McpTool.class)).thenReturn(Set.of(methodA, methodB));
    when(context.getComponentInstance(DuplicateToolComponents.class))
        .thenReturn(new DuplicateToolComponents());
    when(context.getLocalizedString(any(), any()))
        .thenAnswer(invocation -> invocation.getArgument(1));

    McpSyncServer server = mock(McpSyncServer.class);
    McpServerTool registrar = new McpServerTool();

    McpServerComponentRegistrationException exception =
        assertThrows(
            McpServerComponentRegistrationException.class,
            () -> registrar.register(server, context));

    assertTrue(exception.getMessage().contains("Duplicate"));
    assertTrue(exception.getMessage().contains("duplicateTool"));
    verify(server, times(1)).addTool(any());
  }

  @Test
  void registerAsync_shouldRegisterToolsSuccessfully() throws Exception {
    Method method = TestMcpTools.class.getDeclaredMethod("toolWithDefaultName");

    McpApplicationContext context = mock(McpApplicationContext.class);
    when(context.getMethodsAnnotatedWith(McpTool.class)).thenReturn(Set.of(method));
    when(context.getComponentInstance(TestMcpTools.class)).thenReturn(new TestMcpTools());
    when(context.getLocalizedString(any(), any()))
        .thenAnswer(invocation -> invocation.getArgument(1));
    when(context.getFieldsAnnotatedWith(any())).thenReturn(Set.of());

    McpAsyncServer server = mock(McpAsyncServer.class);
    when(server.addTool(any())).thenReturn(Mono.empty());

    McpServerTool registrar = new McpServerTool();

    assertDoesNotThrow(() -> registrar.register(server, context));

    verify(server, times(1)).addTool(any());
  }

  @Test
  void registerAsync_shouldRejectDuplicateToolNames() throws Exception {
    Method methodA = DuplicateToolComponents.class.getDeclaredMethod("toolA");
    Method methodB = DuplicateToolComponents.class.getDeclaredMethod("toolB");

    McpApplicationContext context = mock(McpApplicationContext.class);
    when(context.getMethodsAnnotatedWith(McpTool.class)).thenReturn(Set.of(methodA, methodB));
    when(context.getComponentInstance(DuplicateToolComponents.class))
        .thenReturn(new DuplicateToolComponents());
    when(context.getLocalizedString(any(), any()))
        .thenAnswer(invocation -> invocation.getArgument(1));
    when(context.getFieldsAnnotatedWith(any())).thenReturn(Set.of());

    McpAsyncServer server = mock(McpAsyncServer.class);
    when(server.addTool(any())).thenReturn(Mono.empty());

    McpServerTool registrar = new McpServerTool();

    McpServerComponentRegistrationException exception =
        assertThrows(
            McpServerComponentRegistrationException.class,
            () -> registrar.register(server, context));

    assertTrue(exception.getMessage().contains("Duplicate"));
    assertTrue(exception.getMessage().contains("duplicateTool"));
    verify(server, times(1)).addTool(any());
  }

  @Test
  void registerAsync_shouldWrapRegistrationFailure() throws Exception {
    Method method = TestMcpTools.class.getDeclaredMethod("toolWithDefaultName");

    McpApplicationContext context = mock(McpApplicationContext.class);
    when(context.getMethodsAnnotatedWith(McpTool.class)).thenReturn(Set.of(method));
    when(context.getComponentInstance(TestMcpTools.class)).thenReturn(new TestMcpTools());
    when(context.getLocalizedString(any(), any()))
        .thenAnswer(invocation -> invocation.getArgument(1));
    when(context.getFieldsAnnotatedWith(any())).thenReturn(Set.of());

    McpAsyncServer server = mock(McpAsyncServer.class);
    when(server.addTool(any())).thenReturn(Mono.error(new RuntimeException("addTool failed")));

    McpServerTool registrar = new McpServerTool();

    McpServerComponentRegistrationException exception =
        assertThrows(
            McpServerComponentRegistrationException.class,
            () -> registrar.register(server, context));

    assertTrue(exception.getMessage().contains("Failed to register async McpTool"));
    assertTrue(exception.getMessage().contains("toolWithDefaultName"));
    assertTrue(exception.getCause() instanceof RuntimeException);
  }
}
