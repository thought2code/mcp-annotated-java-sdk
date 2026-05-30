package com.github.thought2code.mcp.annotated.server.component;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.annotation.McpResource;
import com.github.thought2code.mcp.annotated.test.TestMcpResources;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import java.lang.reflect.Method;
import java.util.Set;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class McpServerResourceRegistrationTest {

  @Test
  void fromAsync_shouldCreateSpecificationAndInvokeHandler() throws Exception {
    Method method = TestMcpResources.class.getDeclaredMethod("resource1");

    McpApplicationContext context = mock(McpApplicationContext.class);
    when(context.getComponentInstance(TestMcpResources.class)).thenReturn(new TestMcpResources());

    McpServerResource registrar = new McpServerResource();
    McpServerFeatures.AsyncResourceSpecification specification =
        registrar.fromAsync(method, context);

    assertEquals("test://resource1", specification.resource().uri());
    assertEquals("resource1_name", specification.resource().name());
    assertEquals("resource1_title", specification.resource().title());
    assertEquals("resource1_description", specification.resource().description());
    assertEquals("text/plain", specification.resource().mimeType());

    McpSchema.ReadResourceRequest request =
        McpSchema.ReadResourceRequest.builder("test://resource1").build();
    McpSchema.ReadResourceResult result = specification.readHandler().apply(null, request).block();

    assertEquals(1, result.contents().size());
    McpSchema.TextResourceContents contents =
        (McpSchema.TextResourceContents) result.contents().get(0);
    assertEquals("test://resource1", contents.uri());
    assertEquals("text/plain", contents.mimeType());
    assertEquals("resource1_content", contents.text());
  }

  @Test
  void registerAsync_shouldRegisterResourcesSuccessfully() throws Exception {
    Method method = TestMcpResources.class.getDeclaredMethod("resource1");

    McpApplicationContext context = mock(McpApplicationContext.class);
    when(context.getMethodsAnnotatedWith(McpResource.class)).thenReturn(Set.of(method));
    when(context.getComponentInstance(TestMcpResources.class)).thenReturn(new TestMcpResources());

    McpAsyncServer server = mock(McpAsyncServer.class);
    when(server.addResource(any())).thenReturn(Mono.empty());

    McpServerResource registrar = new McpServerResource();

    assertDoesNotThrow(() -> registrar.register(server, context));

    verify(server, times(1)).addResource(any());
  }
}
