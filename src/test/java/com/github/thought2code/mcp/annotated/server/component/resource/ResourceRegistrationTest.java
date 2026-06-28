package com.github.thought2code.mcp.annotated.server.component.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.github.thought2code.mcp.annotated.enums.MimeType;
import com.github.thought2code.mcp.annotated.exception.McpServerComponentRegistrationException;
import com.github.thought2code.mcp.annotated.server.component.ComponentProvider;
import com.github.thought2code.mcp.annotated.server.component.Invocation;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class ResourceRegistrationTest {

  @Test
  void registerSync_shouldReturnFalseWhenNoComponentProvider() {
    McpSyncServer server = mock(McpSyncServer.class);
    McpApplicationContext context = mock(McpApplicationContext.class);

    boolean registered = ResourceRegistration.registerSync(server, context, List.of());

    assertFalse(registered);
    verify(server, times(0)).addResource(any());
  }

  @Test
  void registerSync_shouldRegisterResource() {
    McpSyncServer server = mock(McpSyncServer.class);
    McpApplicationContext context = mock(McpApplicationContext.class);
    ComponentProvider provider =
        new ComponentProvider() {
          @Override
          public List<ResourceDefinition> resources() {
            McpSchema.Resource resource =
                McpSchema.Resource.builder("test://uri", "component_resource").build();
            return List.of(
                new ResourceDefinition(
                    "test.Source#resource()",
                    resource,
                    ctx -> Invocation.builder().result("ok").build()));
          }
        };
    when(context.isInScope(anyString())).thenReturn(true);

    boolean registered = ResourceRegistration.registerSync(server, context, List.of(provider));

    assertTrue(registered);
    verify(server, times(1)).addResource(any());
  }

  @Test
  void registerSync_shouldRejectDuplicateResourceNames() {
    McpSyncServer server = mock(McpSyncServer.class);
    McpApplicationContext context = mock(McpApplicationContext.class);
    ResourceDefinition a =
        new ResourceDefinition(
            "test.Source#a()",
            McpSchema.Resource.builder("test://a", "duplicate").build(),
            ctx -> Invocation.builder().result("a").build());
    ResourceDefinition b =
        new ResourceDefinition(
            "test.Source#b()",
            McpSchema.Resource.builder("test://b", "duplicate").build(),
            ctx -> Invocation.builder().result("b").build());
    ComponentProvider provider =
        new ComponentProvider() {
          @Override
          public List<ResourceDefinition> resources() {
            return List.of(a, b);
          }
        };
    when(context.isInScope(anyString())).thenReturn(true);

    McpServerComponentRegistrationException exception =
        assertThrows(
            McpServerComponentRegistrationException.class,
            () -> ResourceRegistration.registerSync(server, context, List.of(provider)));
    assertTrue(exception.getMessage().contains("Duplicate resource name 'duplicate'"));
  }

  @Test
  void registerAsync_shouldRegisterResource() {
    McpAsyncServer server = mock(McpAsyncServer.class);
    when(server.addResource(any())).thenReturn(Mono.empty());
    McpApplicationContext context = mock(McpApplicationContext.class);
    ComponentProvider provider =
        new ComponentProvider() {
          @Override
          public List<ResourceDefinition> resources() {
            McpSchema.Resource resource =
                McpSchema.Resource.builder("test://uri", "component_resource_async").build();
            return List.of(
                new ResourceDefinition(
                    "test.Source#resource()",
                    resource,
                    ctx -> Invocation.builder().result("ok").build()));
          }
        };
    when(context.isInScope(anyString())).thenReturn(true);

    boolean registered = ResourceRegistration.registerAsync(server, context, List.of(provider));

    assertTrue(registered);
    verify(server, times(1)).addResource(any());
  }

  @Test
  void invoke_shouldSerializeJsonResourceContentAsJson() {
    McpSchema.Resource resource =
        McpSchema.Resource.builder("test://json", "json_resource")
            .mimeType(MimeType.APPLICATION_JSON.getValue())
            .build();

    McpSchema.ReadResourceResult result =
        invokeResource(
            resource, ctx -> Invocation.builder().result(new Payload("codex", 2)).build());

    McpSchema.TextResourceContents content =
        (McpSchema.TextResourceContents) result.contents().get(0);
    assertEquals(MimeType.APPLICATION_JSON.getValue(), content.mimeType());
    assertEquals("{\"name\":\"codex\",\"count\":2}", content.text());
  }

  @Test
  void invoke_shouldKeepPlainTextResourceContentAsString() {
    McpSchema.Resource resource =
        McpSchema.Resource.builder("test://plain", "plain_resource")
            .mimeType(MimeType.TEXT_PLAIN.getValue())
            .build();

    McpSchema.ReadResourceResult result =
        invokeResource(
            resource, ctx -> Invocation.builder().result(new Payload("codex", 2)).build());

    McpSchema.TextResourceContents content =
        (McpSchema.TextResourceContents) result.contents().get(0);
    assertEquals(MimeType.TEXT_PLAIN.getValue(), content.mimeType());
    assertEquals("Payload[name=codex, count=2]", content.text());
  }

  private static McpSchema.ReadResourceResult invokeResource(
      McpSchema.Resource resource, ResourceInvoker invoker) {
    try {
      Method method =
          ResourceRegistration.class.getDeclaredMethod(
              "invoke",
              ResourceInvoker.class,
              McpApplicationContext.class,
              McpSchema.Resource.class,
              String.class);
      method.setAccessible(true);
      return (McpSchema.ReadResourceResult)
          method.invoke(
              null, invoker, mock(McpApplicationContext.class), resource, "test.Source#resource()");
    } catch (ReflectiveOperationException e) {
      throw new AssertionError(e);
    }
  }

  private record Payload(String name, int count) {}
}
