package com.github.thought2code.mcp.annotated.server.component;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class InvocationLogMessageHelperTest {

  @ParameterizedTest
  @MethodSource("invocationMessageCases")
  void invocationLogMessageConstants_shouldMatchExpectedText(String key, String expected) {
    String actual =
        switch (key) {
          case "tool" -> InvocationLogMessageHelper.TOOL_INVOCATION_FAILED;
          case "prompt" -> InvocationLogMessageHelper.PROMPT_INVOCATION_FAILED;
          case "resource" -> InvocationLogMessageHelper.RESOURCE_INVOCATION_FAILED;
          case "completion" -> InvocationLogMessageHelper.COMPLETION_INVOCATION_FAILED;
          default -> throw new IllegalArgumentException("Unsupported key: " + key);
        };
    assertEquals(expected, actual);
  }

  private static Stream<Arguments> invocationMessageCases() {
    return Stream.of(
        Arguments.of("tool", "Tool invocation failed for sourceMethod={}"),
        Arguments.of("prompt", "Prompt invocation failed for sourceMethod={}"),
        Arguments.of("resource", "Resource invocation failed for sourceMethod={}"),
        Arguments.of("completion", "Completion invocation failed for sourceMethod={}"));
  }
}
