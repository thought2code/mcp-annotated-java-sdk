package com.github.thought2code.mcp.annotated.server.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class AnnotationProcessorCompileSuccessTest {

  @Test
  void processor_shouldGenerateProviderAndServiceDescriptorForValidComponents() throws IOException {
    CompilationResult result = compile(validComponentSource());

    assertTrue(result.success(), () -> "Unexpected diagnostics:\n" + result.messages());
    assertTrue(
        result.generatedProviderSource().contains("implements ComponentProvider"),
        "Expected generated provider implementation");
    assertTrue(
        result
            .generatedProviderSource()
            .contains("private static ToolDefinition toolDefinition0()"),
        "Expected generated tool definition");
    assertTrue(
        result
            .generatedProviderSource()
            .contains("private static Map<String, Object> inputSchema0()"),
        "Expected generated input schema");
    assertTrue(
        result.generatedProviderSource().contains("definitions.put(\"Payload\", definition)"),
        "Expected generated nested input schema definition");
    assertTrue(
        result.generatedProviderSource().contains("fieldProperties.put(\"type\", \"integer\")"),
        "Expected generated output schema properties");
    assertTrue(
        result
            .generatedProviderSource()
            .contains("private static PromptDefinition promptDefinition0()"),
        "Expected generated prompt definition");
    assertTrue(
        result
            .generatedProviderSource()
            .contains("private static ResourceDefinition resourceDefinition0()"),
        "Expected generated resource definition");
    assertTrue(
        result.generatedProviderSource().contains(".mimeType(\"application/json\")"),
        "Expected generated resource MIME type");
    assertTrue(
        result
            .generatedProviderSource()
            .contains("private static CompletionDefinition completionDefinition0()"),
        "Expected generated completion definitions");
    assertTrue(
        result
            .generatedProviderSource()
            .contains("McpSchema.PromptReference.builder(\"write_summary\")"),
        "Expected generated prompt completion reference");
    assertTrue(
        result
            .generatedProviderSource()
            .contains("new McpSchema.ResourceReference(\"fixture://resource/{id}\")"),
        "Expected generated resource completion reference");
    assertEquals(
        result.generatedProviderName(),
        result.serviceDescriptor(),
        "Service descriptor should point to the generated provider");
  }

  private static CompilationResult compile(String sourceCode) throws IOException {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    Assumptions.assumeTrue(compiler != null, "JDK compiler is required to run this test");

    Path tempDir = Files.createTempDirectory("annotation-processor-compile-success");
    Path classesDir = tempDir.resolve("classes");
    Path generatedSourcesDir = tempDir.resolve("generated-sources");
    Path sourceFile = tempDir.resolve("testfixtures/GeneratedProviderFixture.java");
    Files.createDirectories(sourceFile.getParent());
    Files.createDirectories(classesDir);
    Files.createDirectories(generatedSourcesDir);
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
              "-d",
              classesDir.toString(),
              "-s",
              generatedSourcesDir.toString(),
              "-source",
              "17",
              "-target",
              "17");

      JavaCompiler.CompilationTask task =
          compiler.getTask(null, fileManager, diagnostics, options, null, units);
      task.setProcessors(List.of(new AnnotationProcessor()));
      boolean success = task.call();
      String messages =
          diagnostics.getDiagnostics().stream()
              .map(diagnostic -> diagnostic.getKind() + ": " + diagnostic.getMessage(Locale.ROOT))
              .collect(Collectors.joining("\n"));
      Path providerSource = generatedProviderSource(generatedSourcesDir);
      String provider = success ? Files.readString(providerSource) : "";
      String providerName =
          success
              ? "com.github.thought2code.mcp.annotated.generated."
                  + providerSource.getFileName().toString().replace(".java", "")
              : "";
      Path serviceDescriptor =
          classesDir.resolve(
              "META-INF/services/com.github.thought2code.mcp.annotated.server.component.ComponentProvider");
      String serviceDescriptorContent =
          success && Files.exists(serviceDescriptor)
              ? Files.readString(serviceDescriptor).trim()
              : "";
      return new CompilationResult(
          success, messages, providerName, provider, serviceDescriptorContent);
    } finally {
      deleteRecursively(tempDir);
    }
  }

  private static Path generatedProviderSource(Path generatedSourcesDir) throws IOException {
    try (var stream = Files.walk(generatedSourcesDir)) {
      return stream
          .filter(path -> path.getFileName().toString().startsWith("GeneratedComponentProvider_"))
          .filter(path -> path.getFileName().toString().endsWith(".java"))
          .findFirst()
          .orElseThrow(() -> new AssertionError("Generated provider source was not created"));
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

  private static String validComponentSource() {
    return """
        package testfixtures;

        import com.github.thought2code.mcp.annotated.annotation.McpJsonSchemaDefinition;
        import com.github.thought2code.mcp.annotated.annotation.McpJsonSchemaProperty;
        import com.github.thought2code.mcp.annotated.annotation.McpPrompt;
        import com.github.thought2code.mcp.annotated.annotation.McpPromptCompletion;
        import com.github.thought2code.mcp.annotated.annotation.McpPromptParam;
        import com.github.thought2code.mcp.annotated.annotation.McpResource;
        import com.github.thought2code.mcp.annotated.annotation.McpResourceCompletion;
        import com.github.thought2code.mcp.annotated.annotation.McpTool;
        import com.github.thought2code.mcp.annotated.annotation.McpToolParam;
        import com.github.thought2code.mcp.annotated.enums.MimeType;
        import com.github.thought2code.mcp.annotated.server.component.completion.CompletionResult;
        import io.modelcontextprotocol.spec.McpSchema;
        import java.util.List;

        public class GeneratedProviderFixture {
          @McpTool(name = "inspect_payload", title = "Inspect Payload", description = "Inspect payload")
          public ToolOutput inspectPayload(
              @McpToolParam(name = "payload", description = "Payload", required = true) Payload payload,
              @McpToolParam(name = "count", description = "Count", required = true) int count,
              String unannotated) {
            return new ToolOutput("ok", count);
          }

          @McpPrompt(name = "write_summary", title = "Write Summary", description = "Write a summary")
          public String writeSummary(
              @McpPromptParam(name = "topic", description = "Topic", required = true) String topic,
              int unannotated) {
            return topic;
          }

          @McpResource(
              uri = "fixture://resource/{id}",
              name = "fixture_resource",
              title = "Fixture Resource",
              description = "Fixture resource",
              mimeType = MimeType.APPLICATION_JSON,
              roles = {McpSchema.Role.USER},
              priority = 0.75)
          public String resource() {
            return "resource";
          }

          @McpPromptCompletion(name = "write_summary", title = "Summary completion")
          public CompletionResult completePrompt(McpSchema.CompleteRequest.CompleteArgument argument) {
            return CompletionResult.builder().values(List.of("Java")).total(1).hasMore(false).build();
          }

          @McpResourceCompletion(uri = "fixture://resource/{id}")
          public CompletionResult completeResource(McpSchema.CompleteRequest.CompleteArgument argument) {
            return CompletionResult.empty();
          }

          @McpJsonSchemaDefinition
          public static class Payload {
            @McpJsonSchemaProperty(description = "Payload name")
            public String name;

            @McpJsonSchemaProperty(name = "enabled", description = "Whether enabled", required = false)
            public boolean enabled;
          }

          public static class ToolOutput {
            @McpJsonSchemaProperty(description = "Status")
            public String status;

            @McpJsonSchemaProperty(description = "Count")
            public int count;

            public ToolOutput(String status, int count) {
              this.status = status;
              this.count = count;
            }
          }
        }
        """;
  }

  private record CompilationResult(
      boolean success,
      String messages,
      String generatedProviderName,
      String generatedProviderSource,
      String serviceDescriptor) {}
}
