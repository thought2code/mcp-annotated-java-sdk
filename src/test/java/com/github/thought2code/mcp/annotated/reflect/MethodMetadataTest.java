package com.github.thought2code.mcp.annotated.reflect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class MethodMetadataTest {

  @Test
  void of_shouldCacheMetadataByMethodSignature() throws Exception {
    Method method = SampleMethods.class.getDeclaredMethod("echo", String.class);
    MethodMetadata first = MethodMetadata.of(method);
    MethodMetadata second = MethodMetadata.of(method);

    assertSame(first, second);
    assertEquals(1, first.getParameters().length);
    assertEquals(String.class, first.getReturnType());
  }

  static class SampleMethods {
    String echo(String value) {
      return value;
    }
  }
}
