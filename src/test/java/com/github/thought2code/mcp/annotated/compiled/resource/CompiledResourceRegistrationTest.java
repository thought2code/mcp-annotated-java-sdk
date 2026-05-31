package com.github.thought2code.mcp.annotated.compiled.resource;

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

class CompiledResourceRegistrationTest {

  @Test
  void registerSync_shouldReturnFalseWhenNoCompiledProvider() {
    McpSyncServer server = mock(McpSyncServer.class);
    McpApplicationContext context = mock(McpApplicationContext.class);

    boolean registered = CompiledResourceRegistration.registerSync(server, context, List.of());

    assertFalse(registered);
    verify(server, times(0)).addResource(any());
  }

  @Test
  void registerSync_shouldRegisterCompiledResource() {
    McpSyncServer server = mock(McpSyncServer.class);
    McpApplicationContext context = mock(McpApplicationContext.class);
    McpCompiledModelProvider provider =
        new McpCompiledModelProvider() {
          @Override
          public List<CompiledResourceDefinition> resources() {
            McpSchema.Resource resource =
                McpSchema.Resource.builder("test://uri", "compiled_resource").build();
            return List.of(
                new CompiledResourceDefinition(
                    "test.Source#resource()",
                    resource,
                    ctx -> Invocation.builder().result("ok").build()));
          }
        };
    when(context.isInScope(anyString())).thenReturn(true);

    boolean registered =
        CompiledResourceRegistration.registerSync(server, context, List.of(provider));

    assertTrue(registered);
    verify(server, times(1)).addResource(any());
  }

  @Test
  void registerSync_shouldRejectDuplicateResourceNames() {
    McpSyncServer server = mock(McpSyncServer.class);
    McpApplicationContext context = mock(McpApplicationContext.class);
    CompiledResourceDefinition a =
        new CompiledResourceDefinition(
            "test.Source#a()",
            McpSchema.Resource.builder("test://a", "duplicate").build(),
            ctx -> Invocation.builder().result("a").build());
    CompiledResourceDefinition b =
        new CompiledResourceDefinition(
            "test.Source#b()",
            McpSchema.Resource.builder("test://b", "duplicate").build(),
            ctx -> Invocation.builder().result("b").build());
    McpCompiledModelProvider provider =
        new McpCompiledModelProvider() {
          @Override
          public List<CompiledResourceDefinition> resources() {
            return List.of(a, b);
          }
        };
    when(context.isInScope(anyString())).thenReturn(true);

    assertThrows(
        McpServerComponentRegistrationException.class,
        () -> CompiledResourceRegistration.registerSync(server, context, List.of(provider)));
  }

  @Test
  void registerAsync_shouldRegisterCompiledResource() {
    McpAsyncServer server = mock(McpAsyncServer.class);
    when(server.addResource(any())).thenReturn(Mono.empty());
    McpApplicationContext context = mock(McpApplicationContext.class);
    McpCompiledModelProvider provider =
        new McpCompiledModelProvider() {
          @Override
          public List<CompiledResourceDefinition> resources() {
            McpSchema.Resource resource =
                McpSchema.Resource.builder("test://uri", "compiled_resource_async").build();
            return List.of(
                new CompiledResourceDefinition(
                    "test.Source#resource()",
                    resource,
                    ctx -> Invocation.builder().result("ok").build()));
          }
        };
    when(context.isInScope(anyString())).thenReturn(true);

    boolean registered =
        CompiledResourceRegistration.registerAsync(server, context, List.of(provider));

    assertTrue(registered);
    verify(server, times(1)).addResource(any());
  }
}
