package com.github.thought2code.mcp.annotated.server.component.tool;

import com.github.thought2code.mcp.annotated.annotation.McpJsonSchemaDefinition;
import com.github.thought2code.mcp.annotated.annotation.McpToolParam;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;

/**
 * Emits generated Java source for {@code @McpTool} bindings.
 *
 * <p>Methods are written in a stable order (definitions, input schemas, output schemas, invokers)
 * so regenerated providers produce deterministic diffs. {@link Support} abstracts annotation-model
 * details implemented by {@link
 * com.github.thought2code.mcp.annotated.server.component.AnnotationProcessor}.
 *
 * @author codeboyzhou
 */
public final class ToolCodegen {

  private ToolCodegen() {}

  /**
   * One JSON Schema property on a tool output type.
   *
   * @param name property name in the output schema
   * @param type JSON Schema type string
   * @param description property description
   * @param required whether the property is required
   */
  public record PropertySpec(String name, String type, String description, boolean required) {}

  /** Annotation-processing callbacks used while generating tool source. */
  public interface Support {
    /** Fully qualified source-method id for diagnostics. */
    String sourceMethod(ExecutableElement method);

    /** Resolved MCP tool name. */
    String toolName(ExecutableElement method);

    /** Resolved MCP tool title. */
    String toolTitle(ExecutableElement method);

    /** Resolved MCP tool description. */
    String toolDescription(ExecutableElement method);

    /** Escapes a string for inclusion in generated Java string literals. */
    String escape(String value);

    /** Erased Java type name for a mirror. */
    String erasedType(javax.lang.model.type.TypeMirror mirror);

    /**
     * Resolves a reference type mirror to a {@link TypeElement}, or {@code null} for primitives.
     */
    TypeElement asTypeElement(javax.lang.model.type.TypeMirror mirror);

    /** Maps an erased Java type name to a JSON Schema type string. */
    String toJsonSchemaType(String javaType);

    /**
     * Writes a nested {@code #/definitions/...} entry into a schema map in generated code.
     *
     * @throws IOException when writing fails
     */
    void writeDefinitionLiteral(
        Writer writer,
        String indent,
        String targetMap,
        String definitionName,
        TypeElement definitionType)
        throws IOException;

    /** Collects output-schema properties from a {@code @McpJsonSchemaProperty}-annotated type. */
    List<PropertySpec> schemaProperties(TypeElement type);

    /** Java type used in generated invoker local variable declarations. */
    String parameterDeclarationType(javax.lang.model.type.TypeMirror mirror);

    /**
     * {@code Foo.class} literal for {@link
     * com.github.thought2code.mcp.annotated.util.TypeConverter}.
     */
    String classLiteral(javax.lang.model.type.TypeMirror mirror);
  }

  /**
   * Writes all tool sections for the given annotated methods.
   *
   * @param writer generated source writer
   * @param methods sorted {@code @McpTool} methods
   * @param support annotation-model support
   * @throws IOException when writing fails
   */
  public static void writeSections(Writer writer, List<ExecutableElement> methods, Support support)
      throws IOException {
    for (int i = 0; i < methods.size(); i++) {
      writeToolDefinitionMethod(writer, methods.get(i), i, support);
    }
    for (int i = 0; i < methods.size(); i++) {
      writeInputSchemaMethod(writer, methods.get(i), i, support);
    }
    for (int i = 0; i < methods.size(); i++) {
      writeOutputSchemaMethod(writer, methods.get(i), i, support);
    }
    for (int i = 0; i < methods.size(); i++) {
      writeInvoker(writer, methods.get(i), i, support);
    }
  }

  /** Emits {@code toolDefinitionN()} for one annotated method. */
  private static void writeToolDefinitionMethod(
      Writer writer, ExecutableElement method, int index, Support support) throws IOException {
    String sourceMethod = support.sourceMethod(method);
    String name = support.toolName(method);
    String title = support.toolTitle(method);
    String description = support.toolDescription(method);

    writer.write("  private static ToolDefinition toolDefinition" + index + "() {\n");
    writer.write("    Map<String, Object> inputSchema = inputSchema" + index + "();\n");
    writer.write("    Map<String, Object> outputSchema = outputSchema" + index + "();\n");
    writer.write(
        "    McpSchema.Tool tool = McpSchema.Tool.builder(\""
            + support.escape(name)
            + "\", inputSchema)\n");
    writer.write("        .title(\"" + support.escape(title) + "\")\n");
    writer.write("        .description(\"" + support.escape(description) + "\")\n");
    writer.write("        .outputSchema(outputSchema)\n");
    writer.write("        .build();\n");
    writer.write(
        "    return new ToolDefinition(\""
            + support.escape(sourceMethod)
            + "\", tool, new Invoker"
            + index
            + "());\n");
    writer.write("  }\n\n");
  }

  /** Emits {@code inputSchemaN()} from {@code @McpToolParam} parameters. */
  private static void writeInputSchemaMethod(
      Writer writer, ExecutableElement method, int index, Support support) throws IOException {
    writer.write("  private static Map<String, Object> inputSchema" + index + "() {\n");
    writer.write("    Map<String, Object> properties = new LinkedHashMap<>();\n");
    writer.write("    Map<String, Object> definitions = new LinkedHashMap<>();\n");
    writer.write("    List<String> required = new ArrayList<>();\n");

    List<? extends VariableElement> parameters = method.getParameters();
    for (VariableElement parameter : parameters) {
      McpToolParam toolParam = parameter.getAnnotation(McpToolParam.class);
      if (toolParam == null) {
        continue;
      }
      String paramName = toolParam.name();
      String description = toolParam.description().isBlank() ? paramName : toolParam.description();
      String javaType = support.erasedType(parameter.asType());
      TypeElement typeElement = support.asTypeElement(parameter.asType());

      writer.write("    {\n");
      writer.write("      Map<String, Object> property = new HashMap<>();\n");
      if (typeElement != null && typeElement.getAnnotation(McpJsonSchemaDefinition.class) != null) {
        String definitionName = typeElement.getSimpleName().toString();
        writer.write(
            "      property.put(\"$ref\", \"#/definitions/"
                + support.escape(definitionName)
                + "\");\n");
        support.writeDefinitionLiteral(
            writer, "      ", "definitions", definitionName, typeElement);
      } else {
        writer.write(
            "      property.put(\"type\", \""
                + support.escape(support.toJsonSchemaType(javaType))
                + "\");\n");
        writer.write(
            "      property.put(\"description\", \"" + support.escape(description) + "\");\n");
      }
      writer.write("      properties.put(\"" + support.escape(paramName) + "\", property);\n");
      if (toolParam.required()) {
        writer.write("      required.add(\"" + support.escape(paramName) + "\");\n");
      }
      writer.write("    }\n");
    }

    writer.write("    Map<String, Object> schema = new LinkedHashMap<>();\n");
    writer.write("    schema.put(\"type\", \"object\");\n");
    writer.write("    schema.put(\"properties\", properties);\n");
    writer.write("    schema.put(\"required\", required);\n");
    writer.write("    schema.put(\"additionalProperties\", false);\n");
    writer.write("    schema.put(\"definitions\", definitions);\n");
    writer.write("    return schema;\n");
    writer.write("  }\n\n");
  }

  /** Emits {@code outputSchemaN()} from the method return type schema properties. */
  private static void writeOutputSchemaMethod(
      Writer writer, ExecutableElement method, int index, Support support) throws IOException {
    writer.write("  private static Map<String, Object> outputSchema" + index + "() {\n");
    writer.write("    Map<String, Object> schema = new HashMap<>();\n");
    writer.write("    schema.put(\"type\", \"object\");\n");
    writer.write("    Map<String, Object> properties = new LinkedHashMap<>();\n");
    writer.write("    List<String> required = new ArrayList<>();\n");

    TypeElement returnType = support.asTypeElement(method.getReturnType());
    if (returnType != null) {
      for (PropertySpec property : support.schemaProperties(returnType)) {
        writer.write("    {\n");
        writer.write("      Map<String, Object> fieldProperties = new HashMap<>();\n");
        writer.write(
            "      fieldProperties.put(\"type\", \"" + support.escape(property.type()) + "\");\n");
        writer.write(
            "      fieldProperties.put(\"description\", \""
                + support.escape(property.description())
                + "\");\n");
        writer.write(
            "      properties.put(\""
                + support.escape(property.name())
                + "\", fieldProperties);\n");
        if (property.required()) {
          writer.write("      required.add(\"" + support.escape(property.name()) + "\");\n");
        }
        writer.write("    }\n");
      }
    }

    writer.write("    schema.put(\"properties\", properties);\n");
    writer.write("    schema.put(\"required\", required);\n");
    writer.write("    return schema;\n");
    writer.write("  }\n\n");
  }

  /** Emits a private {@code ToolInvoker} implementation for one annotated method. */
  private static void writeInvoker(
      Writer writer, ExecutableElement method, int index, Support support) throws IOException {
    String sourceMethod = support.sourceMethod(method);
    TypeElement owner = (TypeElement) method.getEnclosingElement();
    String ownerType = owner.getQualifiedName().toString();
    boolean returnsVoid = method.getReturnType().getKind() == TypeKind.VOID;

    writer.write("  private static final class Invoker" + index + " implements ToolInvoker {\n");
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
      McpToolParam toolParam = parameter.getAnnotation(McpToolParam.class);
      String valueExpr =
          toolParam == null
              ? "TypeConverter.convert(null, " + targetClassLiteral + ")"
              : "TypeConverter.convert(safeArguments.get(\""
                  + support.escape(toolParam.name())
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
        "        log.error(InvocationLogMessageHelper.TOOL_INVOCATION_FAILED, \""
            + support.escape(sourceMethod)
            + "\", e);\n");
    writer.write(
        "        return Invocation.builder().result(McpServerError.METHOD_INVOCATION_ERROR.toString()).isError(true).build();\n");
    writer.write("      }\n");
    writer.write("    }\n");
    writer.write("  }\n\n");
  }
}
