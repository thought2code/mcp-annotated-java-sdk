package com.github.thought2code.mcp.annotated.reflect;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.thought2code.mcp.annotated.enums.McpServerError;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;

class MethodInvokerTest {

  @Test
  void invoke_whenMethodThrows_shouldReturnSanitizedError() throws Exception {
    ThrowingComponent instance = new ThrowingComponent();
    Method method = ThrowingComponent.class.getDeclaredMethod("alwaysFails");
    MethodCache methodCache = MethodCache.of(method);

    Invocation invocation = MethodInvoker.invoke(instance, methodCache, List.of());

    assertTrue(invocation.isError());
    String result = invocation.result().toString();
    assertTrue(result.contains(McpServerError.METHOD_INVOCATION_ERROR.getCode()));
    assertTrue(result.contains(McpServerError.METHOD_INVOCATION_ERROR.getMessage()));
    assertFalse(result.contains("sensitive detail"));
    assertFalse(result.contains("alwaysFails"));
  }

  static class ThrowingComponent {
    String alwaysFails() {
      throw new IllegalStateException("sensitive detail");
    }
  }
}
