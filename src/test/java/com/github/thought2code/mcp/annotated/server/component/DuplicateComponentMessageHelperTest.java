package com.github.thought2code.mcp.annotated.server.component;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.modelcontextprotocol.spec.McpSchema;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DuplicateComponentMessageHelperTest {

  @ParameterizedTest
  @MethodSource("duplicateNameMessageCases")
  void duplicateNameMessage_shouldMatchTemplate(
      String componentKind, String name, String previousMethod, String currentMethod, String expected) {
    String actual =
        switch (componentKind) {
          case "tool" ->
              DuplicateComponentMessageHelper.duplicateToolName(name, previousMethod, currentMethod);
          case "prompt" ->
              DuplicateComponentMessageHelper.duplicatePromptName(
                  name, previousMethod, currentMethod);
          case "resource" ->
              DuplicateComponentMessageHelper.duplicateResourceName(
                  name, previousMethod, currentMethod);
          default -> throw new IllegalArgumentException("Unsupported component kind: " + componentKind);
        };
    assertEquals(expected, actual);
  }

  @ParameterizedTest
  @MethodSource("duplicateCompletionMessageCases")
  void duplicateCompletionMessage_shouldMatchTemplate(
      String referenceDescription, String previousMethod, String currentMethod, String expected) {
    String actual =
        DuplicateComponentMessageHelper.duplicateCompletionReference(
            referenceDescription, previousMethod, currentMethod);
    assertEquals(expected, actual);
  }

  @ParameterizedTest
  @MethodSource("completionReferenceDescriptionCases")
  void completionReferenceDescription_shouldMatchReadableText(
      McpSchema.CompleteReference reference, String expected) {
    String actual = DuplicateComponentMessageHelper.completionReferenceDescription(reference);
    assertEquals(expected, actual);
  }

  private static Stream<Arguments> duplicateNameMessageCases() {
    return Stream.of(
        Arguments.of(
            "tool",
            "duplicate_tool",
            "test.Source#a()",
            "test.Source#b()",
            "Duplicate tool name 'duplicate_tool' found for methods test.Source#a() and test.Source#b()"),
        Arguments.of(
            "prompt",
            "duplicate_prompt",
            "test.Source#a()",
            "test.Source#b()",
            "Duplicate prompt name 'duplicate_prompt' found for methods test.Source#a() and test.Source#b()"),
        Arguments.of(
            "resource",
            "duplicate_resource",
            "test.Source#a()",
            "test.Source#b()",
            "Duplicate resource name 'duplicate_resource' found for methods test.Source#a() and test.Source#b()"));
  }

  private static Stream<Arguments> duplicateCompletionMessageCases() {
    return Stream.of(
        Arguments.of(
            "prompt name 'generateCode'",
            "test.Source#a()",
            "test.Source#b()",
            "Duplicate completion reference prompt name 'generateCode' found for methods test.Source#a() and test.Source#b()"),
        Arguments.of(
            "resource uri 'file://'",
            "test.Source#a()",
            "test.Source#b()",
            "Duplicate completion reference resource uri 'file://' found for methods test.Source#a() and test.Source#b()"));
  }

  private static Stream<Arguments> completionReferenceDescriptionCases() {
    return Stream.of(
        Arguments.of(
            McpSchema.PromptReference.builder("generateCode").build(), "prompt name 'generateCode'"),
        Arguments.of(new McpSchema.ResourceReference("file://"), "resource uri 'file://'"),
        Arguments.of(null, "'null'"));
  }
}
