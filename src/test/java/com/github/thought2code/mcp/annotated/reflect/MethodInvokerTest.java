package com.github.thought2code.mcp.annotated.reflect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.thought2code.mcp.annotated.enums.McpServerError;
import com.github.thought2code.mcp.annotated.exception.McpServerException;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;

class MethodInvokerTest {

  @Test
  void invoke_whenMethodThrows_shouldReturnSanitizedError() throws Exception {
    ThrowingComponent instance = new ThrowingComponent();
    Method method = ThrowingComponent.class.getDeclaredMethod("alwaysFails");
    MethodMetadata metadata = MethodMetadata.of(method);

    Invocation invocation = MethodInvoker.invoke(instance, method, metadata, List.of());

    assertTrue(invocation.isError());
    String result = invocation.asText();
    assertTrue(result.contains(McpServerError.METHOD_INVOCATION_ERROR.getCode()));
    assertTrue(result.contains(McpServerError.METHOD_INVOCATION_ERROR.getMessage()));
    assertFalse(result.contains("sensitive detail"));
    assertFalse(result.contains("alwaysFails"));
  }

  @Test
  void invoke_shouldReturnSuccessMessageForVoidMethods() throws Exception {
    SampleComponent instance = new SampleComponent();
    Method method = SampleComponent.class.getDeclaredMethod("voidMethod");
    Invocation invocation = MethodInvoker.invoke(instance, method, MethodMetadata.of(method));
    assertFalse(invocation.isError());
    assertEquals("The method call succeeded but has a void return type", invocation.asText());
  }

  @Test
  void invoke_shouldReturnNullMessageWhenMethodReturnsNull() throws Exception {
    SampleComponent instance = new SampleComponent();
    Method method = SampleComponent.class.getDeclaredMethod("nullMethod");
    Invocation invocation = MethodInvoker.invoke(instance, method, MethodMetadata.of(method));
    assertFalse(invocation.isError());
    assertEquals("The method call succeeded but the return value is null", invocation.asText());
  }

  @Test
  void invoke_shouldReturnActualResultForNonNullReturn() throws Exception {
    SampleComponent instance = new SampleComponent();
    Method method = SampleComponent.class.getDeclaredMethod("valueMethod");
    Invocation invocation = MethodInvoker.invoke(instance, method, MethodMetadata.of(method));
    assertFalse(invocation.isError());
    assertEquals("ok", invocation.result());
  }

  @Test
  void createInstance_shouldCreateNoArgInstance() {
    Object instance = MethodInvoker.createInstance(SampleComponent.class);
    assertTrue(instance instanceof SampleComponent);
  }

  @Test
  void createInstance_shouldThrowWhenNoDefaultConstructor() {
    McpServerException exception =
        org.junit.jupiter.api.Assertions.assertThrows(
            McpServerException.class,
            () -> MethodInvoker.createInstance(NoDefaultConstructor.class));
    assertTrue(
        exception.getMessage().contains(McpServerError.COMPONENT_INSTANCE_CREATE_ERROR.getCode()));
  }

  static class ThrowingComponent {
    String alwaysFails() {
      throw new IllegalStateException("sensitive detail");
    }
  }

  static class SampleComponent {
    void voidMethod() {}

    String nullMethod() {
      return null;
    }

    String valueMethod() {
      return "ok";
    }
  }

  static class NoDefaultConstructor {
    NoDefaultConstructor(String ignored) {}
  }
}
