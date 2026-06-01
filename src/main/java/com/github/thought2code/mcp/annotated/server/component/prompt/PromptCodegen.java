package com.github.thought2code.mcp.annotated.server.component.prompt;

import com.github.thought2code.mcp.annotated.annotation.McpPromptParam;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;

/** Writes prompt-related generated methods in a deterministic order. */
public final class PromptCodegen {

  private PromptCodegen() {}

  public interface Support {
    String sourceMethod(ExecutableElement method);

    String promptName(ExecutableElement method);

    String promptTitle(ExecutableElement method);

    String promptDescription(ExecutableElement method);

    String escape(String value);

    String parameterDeclarationType(javax.lang.model.type.TypeMirror mirror);

    String classLiteral(javax.lang.model.type.TypeMirror mirror);
  }

  public static void writeSections(Writer writer, List<ExecutableElement> methods, Support support)
      throws IOException {
    for (int i = 0; i < methods.size(); i++) {
      writePromptDefinitionMethod(writer, methods.get(i), i, support);
    }
    for (int i = 0; i < methods.size(); i++) {
      writePromptInvoker(writer, methods.get(i), i, support);
    }
  }

  private static void writePromptDefinitionMethod(
      Writer writer, ExecutableElement method, int index, Support support) throws IOException {
    String sourceMethod = support.sourceMethod(method);
    String name = support.promptName(method);
    String title = support.promptTitle(method);
    String description = support.promptDescription(method);

    writer.write("  private static PromptDefinition promptDefinition" + index + "() {\n");
    writer.write("    List<McpSchema.PromptArgument> args = new ArrayList<>();\n");
    for (VariableElement parameter : method.getParameters()) {
      McpPromptParam promptParam = parameter.getAnnotation(McpPromptParam.class);
      if (promptParam == null) {
        continue;
      }
      String paramName = promptParam.name();
      String paramTitle = promptParam.title().isBlank() ? paramName : promptParam.title();
      String paramDescription =
          promptParam.description().isBlank() ? paramName : promptParam.description();
      writer.write(
          "    args.add(McpSchema.PromptArgument.builder(\""
              + support.escape(paramName)
              + "\")\n"
              + "        .title(\""
              + support.escape(paramTitle)
              + "\")\n"
              + "        .description(\""
              + support.escape(paramDescription)
              + "\")\n"
              + "        .required("
              + promptParam.required()
              + ")\n"
              + "        .build());\n");
    }

    writer.write(
        "    McpSchema.Prompt prompt = McpSchema.Prompt.builder(\""
            + support.escape(name)
            + "\")\n"
            + "        .title(\""
            + support.escape(title)
            + "\")\n"
            + "        .description(\""
            + support.escape(description)
            + "\")\n"
            + "        .arguments(args)\n"
            + "        .build();\n");
    writer.write(
        "    return new PromptDefinition(\""
            + support.escape(sourceMethod)
            + "\", prompt, \""
            + support.escape(description)
            + "\", new PromptInvoker"
            + index
            + "());\n");
    writer.write("  }\n\n");
  }

  private static void writePromptInvoker(
      Writer writer, ExecutableElement method, int index, Support support) throws IOException {
    TypeElement owner = (TypeElement) method.getEnclosingElement();
    String ownerType = owner.getQualifiedName().toString();
    boolean returnsVoid = method.getReturnType().getKind() == TypeKind.VOID;

    writer.write(
        "  private static final class PromptInvoker" + index + " implements PromptInvoker {\n");
    writer.write("    @Override\n");
    writer.write(
        "    public Invocation invoke(McpApplicationContext context, Map<String, Object> arguments) {\n");
    writer.write(
        "      Map<String, Object> safeArguments = arguments == null ? Map.of() : arguments;\n");
    writer.write("      try {\n");
    writer.write(
        "        "
            + ownerType
            + " instance = ("
            + ownerType
            + ") context.getComponentInstance("
            + ownerType
            + ".class);\n");

    List<? extends VariableElement> parameters = method.getParameters();
    List<String> argumentNames = new ArrayList<>(parameters.size());
    for (int i = 0; i < parameters.size(); i++) {
      VariableElement parameter = parameters.get(i);
      String paramType = support.parameterDeclarationType(parameter.asType());
      String targetClassLiteral = support.classLiteral(parameter.asType());
      McpPromptParam promptParam = parameter.getAnnotation(McpPromptParam.class);
      String valueExpr =
          promptParam == null
              ? "TypeConverter.convert(null, " + targetClassLiteral + ")"
              : "TypeConverter.convert(safeArguments.get(\""
                  + support.escape(promptParam.name())
                  + "\"), "
                  + targetClassLiteral
                  + ")";
      String argumentName = "arg" + i;
      argumentNames.add(argumentName);
      writer.write(
          "        "
              + paramType
              + " "
              + argumentName
              + " = ("
              + paramType
              + ") "
              + valueExpr
              + ";\n");
    }

    String joinedArguments = String.join(", ", argumentNames);
    if (returnsVoid) {
      writer.write("        instance." + method.getSimpleName() + "(" + joinedArguments + ");\n");
      writer.write(
          "        return Invocation.builder().result(\"The method call succeeded but has a void return type\").build();\n");
    } else {
      writer.write(
          "        Object result = instance."
              + method.getSimpleName()
              + "("
              + joinedArguments
              + ");\n");
      writer.write(
          "        Object resultIfNull = \"The method call succeeded but the return value is null\";\n");
      writer.write(
          "        return Invocation.builder().result(result == null ? resultIfNull : result).build();\n");
    }
    writer.write("      } catch (Exception e) {\n");
    writer.write(
        "        return Invocation.builder().result(McpServerError.METHOD_INVOCATION_ERROR.toString()).isError(true).build();\n");
    writer.write("      }\n");
    writer.write("    }\n");
    writer.write("  }\n\n");
  }
}
