package com.github.thought2code.mcp.annotated.server.component.completion;

import com.github.thought2code.mcp.annotated.annotation.McpPromptCompletion;
import com.github.thought2code.mcp.annotated.annotation.McpResourceCompletion;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

/** Writes completion-related generated methods in a deterministic order. */
public final class CompletionCodegen {

  private CompletionCodegen() {}

  public interface Support {
    String sourceMethod(ExecutableElement method);

    String escape(String value);
  }

  public static void writeSections(Writer writer, List<ExecutableElement> methods, Support support)
      throws IOException {
    for (int i = 0; i < methods.size(); i++) {
      writeCompletionDefinitionMethod(writer, methods.get(i), i, support);
    }
    for (int i = 0; i < methods.size(); i++) {
      writeCompletionInvoker(writer, methods.get(i), i, support);
    }
  }

  private static void writeCompletionDefinitionMethod(
      Writer writer, ExecutableElement method, int index, Support support) throws IOException {
    String sourceMethod = support.sourceMethod(method);
    writer.write("  private static CompletionDefinition completionDefinition" + index + "() {\n");
    if (method.getAnnotation(McpPromptCompletion.class) != null) {
      McpPromptCompletion annotation = method.getAnnotation(McpPromptCompletion.class);
      writer.write(
          "    McpSchema.CompleteReference reference = McpSchema.PromptReference.builder(\""
              + support.escape(annotation.name())
              + "\")");
      if (!annotation.title().isBlank()) {
        writer.write(".title(\"" + support.escape(annotation.title()) + "\")");
      }
      writer.write(".build();\n");
    } else {
      McpResourceCompletion annotation = method.getAnnotation(McpResourceCompletion.class);
      writer.write(
          "    McpSchema.CompleteReference reference = new McpSchema.ResourceReference(\""
              + support.escape(annotation.uri())
              + "\");\n");
    }
    writer.write(
        "    return new CompletionDefinition(\""
            + support.escape(sourceMethod)
            + "\", reference, new CompletionInvoker"
            + index
            + "());\n");
    writer.write("  }\n\n");
  }

  private static void writeCompletionInvoker(
      Writer writer, ExecutableElement method, int index, Support support) throws IOException {
    String sourceMethod = support.sourceMethod(method);
    TypeElement owner = (TypeElement) method.getEnclosingElement();
    String ownerType = owner.getQualifiedName().toString();

    writer.write(
        "  private static final class CompletionInvoker"
            + index
            + " implements CompletionInvoker {\n");
    writer.write("    @Override\n");
    writer.write(
        "    public Invocation invoke(McpApplicationContext context, McpSchema.CompleteRequest.CompleteArgument argument) {\n");
    writer.write("      try {\n");
    writer.write(
        "        "
            + ownerType
            + " instance = ("
            + ownerType
            + ") context.getComponentInstance("
            + ownerType
            + ".class);\n");
    writer.write("        Object result = instance." + method.getSimpleName() + "(argument);\n");
    writer.write(
        "        Object resultIfNull = \"The method call succeeded but the return value is null\";\n");
    writer.write(
        "        return Invocation.builder().result(result == null ? resultIfNull : result).build();\n");
    writer.write("      } catch (Exception e) {\n");
    writer.write(
        "        log.error(InvocationLogMessageHelper.COMPLETION_INVOCATION_FAILED, \""
            + support.escape(sourceMethod)
            + "\", e);\n");
    writer.write(
        "        return Invocation.builder().result(McpServerError.METHOD_INVOCATION_ERROR.toString()).isError(true).build();\n");
    writer.write("      }\n");
    writer.write("    }\n");
    writer.write("  }\n\n");
  }
}
