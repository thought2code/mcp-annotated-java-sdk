package com.github.thought2code.mcp.annotated.server.component.resource;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;

/**
 * Emits generated Java source for {@code @McpResource} bindings.
 *
 * @author codeboyzhou
 */
public final class ResourceCodegen {

  private ResourceCodegen() {}

  /** Annotation-processing callbacks used while generating resource source. */
  public interface Support {
    /**
     * Fully qualified source-method id for diagnostics.
     *
     * @param method annotated resource method
     * @return diagnostic source-method id
     */
    String sourceMethod(ExecutableElement method);

    /**
     * Resolved resource URI from {@code @McpResource#uri()}.
     *
     * @param method annotated resource method
     * @return resource URI
     */
    String resourceUri(ExecutableElement method);

    /**
     * Resolved MCP resource name.
     *
     * @param method annotated resource method
     * @return resource name
     */
    String resourceName(ExecutableElement method);

    /**
     * Resolved MCP resource title.
     *
     * @param method annotated resource method
     * @return resource title
     */
    String resourceTitle(ExecutableElement method);

    /**
     * Resolved MCP resource description.
     *
     * @param method annotated resource method
     * @return resource description
     */
    String resourceDescription(ExecutableElement method);

    /**
     * Resolved MIME type.
     *
     * @param method annotated resource method
     * @return resource MIME type
     */
    String resourceMimeType(ExecutableElement method);

    /**
     * Generated {@code List.of(McpSchema.Role...)} literal for annotations.
     *
     * @param method annotated resource method
     * @return generated audience roles literal
     */
    String resourceRolesLiteral(ExecutableElement method);

    /**
     * Resolved annotation priority.
     *
     * @param method annotated resource method
     * @return resource annotation priority
     */
    double resourcePriority(ExecutableElement method);

    /**
     * Escapes a string for inclusion in generated Java string literals.
     *
     * @param value raw string value
     * @return escaped Java string-literal content
     */
    String escape(String value);
  }

  /**
   * Writes all resource sections for the given annotated methods.
   *
   * @param writer generated source writer
   * @param methods sorted {@code @McpResource} methods
   * @param support annotation-model support
   * @throws IOException when writing fails
   */
  public static void writeSections(Writer writer, List<ExecutableElement> methods, Support support)
      throws IOException {
    for (int i = 0; i < methods.size(); i++) {
      writeResourceDefinitionMethod(writer, methods.get(i), i, support);
    }
    for (int i = 0; i < methods.size(); i++) {
      writeResourceInvoker(writer, methods.get(i), i, support);
    }
  }

  /** Emits {@code resourceDefinitionN()} for one annotated method. */
  private static void writeResourceDefinitionMethod(
      Writer writer, ExecutableElement method, int index, Support support) throws IOException {
    String sourceMethod = support.sourceMethod(method);
    String uri = support.resourceUri(method);
    String name = support.resourceName(method);
    String title = support.resourceTitle(method);
    String description = support.resourceDescription(method);
    String mimeType = support.resourceMimeType(method);
    String rolesLiteral = support.resourceRolesLiteral(method);
    double priority = support.resourcePriority(method);

    writer.write("  private static ResourceDefinition resourceDefinition" + index + "() {\n");
    writer.write(
        "    McpSchema.Resource resource = McpSchema.Resource.builder(\""
            + support.escape(uri)
            + "\", \""
            + support.escape(name)
            + "\")\n");
    writer.write("        .title(\"" + support.escape(title) + "\")\n");
    writer.write("        .description(\"" + support.escape(description) + "\")\n");
    writer.write("        .mimeType(\"" + support.escape(mimeType) + "\")\n");
    writer.write(
        "        .annotations(McpSchema.Annotations.builder().audience("
            + rolesLiteral
            + ").priority("
            + priority
            + ").build())\n");
    writer.write("        .build();\n");
    writer.write(
        "    return new ResourceDefinition(\""
            + support.escape(sourceMethod)
            + "\", resource, new ResourceInvoker"
            + index
            + "());\n");
    writer.write("  }\n\n");
  }

  /** Emits a private {@code ResourceInvoker} implementation for one annotated method. */
  private static void writeResourceInvoker(
      Writer writer, ExecutableElement method, int index, Support support) throws IOException {
    String sourceMethod = support.sourceMethod(method);
    TypeElement owner = (TypeElement) method.getEnclosingElement();
    String ownerType = owner.getQualifiedName().toString();
    boolean returnsVoid = method.getReturnType().getKind() == TypeKind.VOID;

    writer.write(
        "  private static final class ResourceInvoker" + index + " implements ResourceInvoker {\n");
    writer.write("    @Override\n");
    writer.write("    public Invocation invoke(McpApplicationContext context) {\n");
    writer.write("      try {\n");
    writer.write(
        "        "
            + ownerType
            + " instance = ("
            + ownerType
            + ") context.getComponentInstance("
            + ownerType
            + ".class);\n");
    if (returnsVoid) {
      writer.write("        instance." + method.getSimpleName() + "();\n");
      writer.write(
          "        return Invocation.builder().result(\"The method call succeeded but has a void return type\").build();\n");
    } else {
      writer.write("        Object result = instance." + method.getSimpleName() + "();\n");
      writer.write(
          "        Object resultIfNull = \"The method call succeeded but the return value is null\";\n");
      writer.write(
          "        return Invocation.builder().result(result == null ? resultIfNull : result).build();\n");
    }
    writer.write("      } catch (Exception e) {\n");
    writer.write(
        "        log.error(InvocationLogMessageHelper.RESOURCE_INVOCATION_FAILED, \""
            + support.escape(sourceMethod)
            + "\", e);\n");
    writer.write(
        "        return Invocation.builder().result(McpServerError.METHOD_INVOCATION_ERROR.toString()).isError(true).build();\n");
    writer.write("      }\n");
    writer.write("    }\n");
    writer.write("  }\n\n");
  }
}
