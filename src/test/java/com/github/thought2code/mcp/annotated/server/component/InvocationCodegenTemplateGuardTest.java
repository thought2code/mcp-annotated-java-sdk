package com.github.thought2code.mcp.annotated.server.component;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class InvocationCodegenTemplateGuardTest {

  @ParameterizedTest
  @MethodSource("codegenReferenceCases")
  void codegenTemplates_shouldReferenceInvocationLogMessageHelper(
      String relativePath, String expectedSnippet) throws IOException {
    Path file = Path.of(relativePath);
    String source = Files.readString(file);
    assertTrue(
        source.contains(expectedSnippet),
        () -> "Expected snippet not found in " + relativePath + ": " + expectedSnippet);
  }

  private static Stream<Arguments> codegenReferenceCases() {
    return Stream.of(
        Arguments.of(
            "src/main/java/com/github/thought2code/mcp/annotated/server/component/tool/ToolCodegen.java",
            "InvocationLogMessageHelper.TOOL_INVOCATION_FAILED"),
        Arguments.of(
            "src/main/java/com/github/thought2code/mcp/annotated/server/component/prompt/PromptCodegen.java",
            "InvocationLogMessageHelper.PROMPT_INVOCATION_FAILED"),
        Arguments.of(
            "src/main/java/com/github/thought2code/mcp/annotated/server/component/resource/ResourceCodegen.java",
            "InvocationLogMessageHelper.RESOURCE_INVOCATION_FAILED"),
        Arguments.of(
            "src/main/java/com/github/thought2code/mcp/annotated/server/component/completion/CompletionCodegen.java",
            "InvocationLogMessageHelper.COMPLETION_INVOCATION_FAILED"),
        Arguments.of(
            "src/main/java/com/github/thought2code/mcp/annotated/server/component/AnnotationProcessor.java",
            "import com.github.thought2code.mcp.annotated.server.component.InvocationLogMessageHelper;"));
  }
}
