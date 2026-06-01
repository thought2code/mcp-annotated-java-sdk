package com.github.thought2code.mcp.annotated.server.component;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AnnotationProcessorCompileFailureTest {

  @ParameterizedTest
  @MethodSource("duplicateDefinitionCases")
  void processor_shouldFailCompilationForDuplicateDefinitions(
      String sourceCode, String expectedMessageFragment) throws IOException {
    CompilationResult result = compile(sourceCode);
    assertFalse(result.success(), "Compilation should fail for duplicate definitions");
    assertTrue(
        result.errorMessages().contains(expectedMessageFragment),
        () ->
            "Expected message fragment not found.\nExpected: "
                + expectedMessageFragment
                + "\nActual errors:\n"
                + result.errorMessages());
  }

  @Test
  void processor_shouldFailCompilationForDuplicateCompletionReference() throws IOException {
    CompilationResult result = compile(duplicateCompletionSource());
    assertFalse(result.success(), "Compilation should fail for duplicate completion reference");
    assertTrue(
        result.errorMessages().contains("Duplicate completion reference prompt name 'dup_prompt'"),
        () -> "Unexpected diagnostics:\n" + result.errorMessages());
  }

  private static List<Arguments> duplicateDefinitionCases() {
    return List.of(
        Arguments.of(duplicateToolSource(), "Duplicate tool name 'dup_tool'"),
        Arguments.of(duplicatePromptSource(), "Duplicate prompt name 'dup_prompt'"),
        Arguments.of(duplicateResourceSource(), "Duplicate resource name 'dup_resource'"));
  }

  private static CompilationResult compile(String sourceCode) throws IOException {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    Assumptions.assumeTrue(compiler != null, "JDK compiler is required to run this test");

    Path tempDir = Files.createTempDirectory("annotation-processor-compile-failure");
    Path sourceFile = tempDir.resolve("testfixtures/DuplicateFixture.java");
    Files.createDirectories(sourceFile.getParent());
    Files.writeString(sourceFile, sourceCode);

    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    try (StandardJavaFileManager fileManager =
        compiler.getStandardFileManager(diagnostics, Locale.ROOT, null)) {
      Iterable<? extends JavaFileObject> units =
          fileManager.getJavaFileObjectsFromFiles(List.of(sourceFile.toFile()));
      List<String> options =
          List.of(
              "-classpath",
              System.getProperty("java.class.path"),
              "-proc:only",
              "-source",
              "17",
              "-target",
              "17");

      JavaCompiler.CompilationTask task =
          compiler.getTask(null, fileManager, diagnostics, options, null, units);
      task.setProcessors(List.of(new AnnotationProcessor()));
      boolean success = task.call();
      String errorMessages =
          diagnostics.getDiagnostics().stream()
              .filter(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR)
              .map(diagnostic -> diagnostic.getMessage(Locale.ROOT))
              .collect(Collectors.joining("\n"));
      return new CompilationResult(success, errorMessages);
    } finally {
      deleteRecursively(tempDir);
    }
  }

  private static void deleteRecursively(Path root) throws IOException {
    List<Path> paths = new ArrayList<>();
    try (var stream = Files.walk(root)) {
      stream.forEach(paths::add);
    }
    for (int i = paths.size() - 1; i >= 0; i--) {
      Files.deleteIfExists(paths.get(i));
    }
  }

  private static String duplicateToolSource() {
    return """
        package testfixtures;

        import com.github.thought2code.mcp.annotated.annotation.McpTool;

        public class DuplicateFixture {
          @McpTool(name = "dup_tool")
          public String first() {
            return "a";
          }

          @McpTool(name = "dup_tool")
          public String second() {
            return "b";
          }
        }
        """;
  }

  private static String duplicatePromptSource() {
    return """
        package testfixtures;

        import com.github.thought2code.mcp.annotated.annotation.McpPrompt;

        public class DuplicateFixture {
          @McpPrompt(name = "dup_prompt")
          public String first() {
            return "a";
          }

          @McpPrompt(name = "dup_prompt")
          public String second() {
            return "b";
          }
        }
        """;
  }

  private static String duplicateResourceSource() {
    return """
        package testfixtures;

        import com.github.thought2code.mcp.annotated.annotation.McpResource;

        public class DuplicateFixture {
          @McpResource(uri = "test://a", name = "dup_resource")
          public String first() {
            return "a";
          }

          @McpResource(uri = "test://b", name = "dup_resource")
          public String second() {
            return "b";
          }
        }
        """;
  }

  private static String duplicateCompletionSource() {
    return """
        package testfixtures;

        import com.github.thought2code.mcp.annotated.annotation.McpPromptCompletion;
        import com.github.thought2code.mcp.annotated.server.component.completion.CompletionResult;
        import io.modelcontextprotocol.spec.McpSchema;

        public class DuplicateFixture {
          @McpPromptCompletion(name = "dup_prompt")
          public CompletionResult first(McpSchema.CompleteRequest.CompleteArgument argument) {
            return CompletionResult.empty();
          }

          @McpPromptCompletion(name = "dup_prompt")
          public CompletionResult second(McpSchema.CompleteRequest.CompleteArgument argument) {
            return CompletionResult.empty();
          }
        }
        """;
  }

  private record CompilationResult(boolean success, String errorMessages) {}
}
