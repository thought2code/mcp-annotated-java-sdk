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

/** Writes tool-related generated methods in a deterministic order. */
public final class ToolCodegen {

  private ToolCodegen() {}

  public record PropertySpec(String name, String type, String description, boolean required) {}

  public interface Support {
    String sourceMethod(ExecutableElement method);

    String toolName(ExecutableElement method);

    String toolTitle(ExecutableElement method);

    String toolDescription(ExecutableElement method);

    String escape(String value);

    String erasedType(javax.lang.model.type.TypeMirror mirror);

    TypeElement asTypeElement(javax.lang.model.type.TypeMirror mirror);

    String toJsonSchemaType(String javaType);

    void writeDefinitionLiteral(
        Writer writer,
        String indent,
        String targetMap,
        String definitionName,
        TypeElement definitionType)
        throws IOException;

    List<PropertySpec> schemaProperties(TypeElement type);

    String parameterDeclarationType(javax.lang.model.type.TypeMirror mirror);

    String classLiteral(javax.lang.model.type.TypeMirror mirror);
  }

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

  private static void writeInvoker(
      Writer writer, ExecutableElement method, int index, Support support) throws IOException {
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
        "        return Invocation.builder().result(McpServerError.METHOD_INVOCATION_ERROR.toString()).isError(true).build();\n");
    writer.write("      }\n");
    writer.write("    }\n");
    writer.write("  }\n\n");
  }
}
