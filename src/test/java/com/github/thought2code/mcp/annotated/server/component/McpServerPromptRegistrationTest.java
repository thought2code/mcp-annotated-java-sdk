package com.github.thought2code.mcp.annotated.server.component;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.annotation.McpPrompt;
import com.github.thought2code.mcp.annotated.test.TestMcpPrompts;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class McpServerPromptRegistrationTest {

  @Test
  void fromAsync_shouldCreateSpecificationAndInvokeHandler() throws Exception {
    Method method = TestMcpPrompts.class.getDeclaredMethod("promptWithDefaultName");

    McpApplicationContext context = mock(McpApplicationContext.class);
    when(context.getComponentInstance(TestMcpPrompts.class)).thenReturn(new TestMcpPrompts());
    when(context.getLocalizedString(any(), any()))
        .thenAnswer(invocation -> invocation.getArgument(1));

    McpServerPrompt registrar = new McpServerPrompt();
    McpServerFeatures.AsyncPromptSpecification specification = registrar.fromAsync(method, context);

    assertEquals("promptWithDefaultName", specification.prompt().name());
    assertEquals("promptWithDefaultName", specification.prompt().title());
    assertEquals("promptWithDefaultName", specification.prompt().description());
    assertTrue(specification.prompt().arguments().isEmpty());

    McpSchema.GetPromptRequest request =
        McpSchema.GetPromptRequest.builder("promptWithDefaultName").build();
    McpSchema.GetPromptResult result = specification.promptHandler().apply(null, request).block();

    assertEquals("promptWithDefaultName", result.description());
    assertEquals(1, result.messages().size());
    assertEquals(McpSchema.Role.USER, result.messages().get(0).role());
    assertEquals(
        "promptWithDefaultName is called",
        ((McpSchema.TextContent) result.messages().get(0).content()).text());
  }

  @Test
  void fromAsync_handler_shouldPassPromptArguments() throws Exception {
    Method method = TestMcpPrompts.class.getDeclaredMethod("promptWithOptionalParam", String.class);

    McpApplicationContext context = mock(McpApplicationContext.class);
    when(context.getComponentInstance(TestMcpPrompts.class)).thenReturn(new TestMcpPrompts());
    when(context.getLocalizedString(any(), any()))
        .thenAnswer(invocation -> invocation.getArgument(1));

    McpServerPrompt registrar = new McpServerPrompt();
    McpServerFeatures.AsyncPromptSpecification specification = registrar.fromAsync(method, context);

    assertEquals(1, specification.prompt().arguments().size());

    McpSchema.GetPromptRequest request =
        McpSchema.GetPromptRequest.builder("promptWithOptionalParam")
            .arguments(Map.of("param", "hello"))
            .build();
    McpSchema.GetPromptResult result = specification.promptHandler().apply(null, request).block();

    assertEquals(
        "promptWithOptionalParam is called with param: hello",
        ((McpSchema.TextContent) result.messages().get(0).content()).text());
  }

  @Test
  void registerAsync_shouldRegisterPromptsSuccessfully() throws Exception {
    Method method = TestMcpPrompts.class.getDeclaredMethod("promptWithDefaultName");

    McpApplicationContext context = mock(McpApplicationContext.class);
    when(context.getMethodsAnnotatedWith(McpPrompt.class)).thenReturn(Set.of(method));
    when(context.getComponentInstance(TestMcpPrompts.class)).thenReturn(new TestMcpPrompts());
    when(context.getLocalizedString(any(), any()))
        .thenAnswer(invocation -> invocation.getArgument(1));

    McpAsyncServer server = mock(McpAsyncServer.class);
    when(server.addPrompt(any())).thenReturn(Mono.empty());

    McpServerPrompt registrar = new McpServerPrompt();

    assertDoesNotThrow(() -> registrar.register(server, context));

    verify(server, times(1)).addPrompt(any());
  }
}
