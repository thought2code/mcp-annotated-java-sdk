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

/**
 * Emits generated Java source for {@code @McpPrompt} bindings.
 *
 * @author codeboyzhou
 */
public final class PromptCodegen {

  private PromptCodegen() {}

  /** Annotation-processing callbacks used while generating prompt source. */
  public interface Support {
    /**
     * Fully qualified source-method id for diagnostics.
     *
     * @param method annotated prompt method
     * @return diagnostic source-method id
     */
    String sourceMethod(ExecutableElement method);

    /**
     * Resolved MCP prompt name.
     *
     * @param method annotated prompt method
     * @return prompt name
     */
    String promptName(ExecutableElement method);

    /**
     * Resolved MCP prompt title.
     *
     * @param method annotated prompt method
     * @return prompt title
     */
    String promptTitle(ExecutableElement method);

    /**
     * Resolved MCP prompt description.
     *
     * @param method annotated prompt method
     * @return prompt description
     */
    String promptDescription(ExecutableElement method);

    /**
     * Escapes a string for inclusion in generated Java string literals.
     *
     * @param value raw string value
     * @return escaped Java string-literal content
     */
    String escape(String value);

    /**
     * Java type used in generated invoker local variable declarations.
     *
     * @param mirror parameter type mirror
     * @return generated local-variable declaration type
     */
    String parameterDeclarationType(javax.lang.model.type.TypeMirror mirror);

    /**
     * {@code Foo.class} literal for {@link
     * com.github.thought2code.mcp.annotated.util.TypeConverter}.
     *
     * @param mirror parameter type mirror
     * @return generated class literal
     */
    String classLiteral(javax.lang.model.type.TypeMirror mirror);
  }

  /**
   * Writes all prompt sections for the given annotated methods.
   *
   * @param writer generated source writer
   * @param methods sorted {@code @McpPrompt} methods
   * @param support annotation-model support
   * @throws IOException when writing fails
   */
  public static void writeSections(Writer writer, List<ExecutableElement> methods, Support support)
      throws IOException {
    for (int i = 0; i < methods.size(); i++) {
      writePromptDefinitionMethod(writer, methods.get(i), i, support);
    }
    for (int i = 0; i < methods.size(); i++) {
      writePromptInvoker(writer, methods.get(i), i, support);
    }
  }

  /** Emits {@code promptDefinitionN()} for one annotated method. */
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

  /** Emits a private {@code PromptInvoker} implementation for one annotated method. */
  private static void writePromptInvoker(
      Writer writer, ExecutableElement method, int index, Support support) throws IOException {
    String sourceMethod = support.sourceMethod(method);
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
        "        log.error(InvocationLogMessageHelper.PROMPT_INVOCATION_FAILED, \""
            + support.escape(sourceMethod)
            + "\", e);\n");
    writer.write(
        "        return Invocation.builder().result(McpServerError.METHOD_INVOCATION_ERROR.toString()).isError(true).build();\n");
    writer.write("      }\n");
    writer.write("    }\n");
    writer.write("  }\n\n");
  }
}
