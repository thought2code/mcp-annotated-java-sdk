package com.github.thought2code.mcp.annotated.reflect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.thought2code.mcp.annotated.annotation.McpToolParam;
import com.github.thought2code.mcp.annotated.enums.McpServerError;
import com.github.thought2code.mcp.annotated.exception.McpServerException;
import com.github.thought2code.mcp.annotated.server.converter.McpToolParameterConverter;
import java.lang.reflect.Method;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ComponentInvocationSupportTest {

  private final McpToolParameterConverter converter = new McpToolParameterConverter();

  @Test
  void invokeWithParameters_shouldConvertArgumentsAndInvokeMethod() throws Exception {
    ToolComponent component = new ToolComponent();
    Method method = ToolComponent.class.getDeclaredMethod("greet", String.class);

    Invocation invocation =
        ComponentInvocationSupport.invokeWithParameters(
            component, method, converter, Map.of("name", "Alice"));

    assertFalse(invocation.isError());
    assertEquals("Hello Alice", invocation.result());
  }

  @Test
  void invokeWithParameters_shouldReturnSanitizedErrorWhenConversionFails() throws Exception {
    ToolComponent component = new ToolComponent();
    Method method = ToolComponent.class.getDeclaredMethod("needsInt", int.class);

    Invocation invocation =
        ComponentInvocationSupport.invokeWithParameters(
            component, method, converter, Map.of("value", "not-a-number"));

    assertTrue(invocation.isError());
    assertEquals(McpServerError.METHOD_INVOCATION_ERROR.toString(), invocation.asText());
  }

  @Test
  void throwIfError_shouldThrowMcpServerExceptionForErrorInvocation() {
    Invocation error =
        Invocation.builder()
            .result(McpServerError.METHOD_INVOCATION_ERROR.toString())
            .isError(true)
            .build();
    assertThrows(McpServerException.class, () -> ComponentInvocationSupport.throwIfError(error));
  }

  @Test
  void requireResult_shouldReturnTypedResultWhenSuccessful() {
    Invocation success = Invocation.builder().result("value").build();
    assertEquals("value", ComponentInvocationSupport.requireResult(success, String.class));
  }

  static class ToolComponent {
    public String greet(@McpToolParam(name = "name") String name) {
      return "Hello " + name;
    }

    public int needsInt(@McpToolParam(name = "value") int value) {
      return value;
    }
  }
}
