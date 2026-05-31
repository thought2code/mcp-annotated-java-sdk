package com.github.thought2code.mcp.annotated.compiler;

import com.github.thought2code.mcp.annotated.annotation.McpJsonSchemaDefinition;
import com.github.thought2code.mcp.annotated.annotation.McpJsonSchemaProperty;
import com.github.thought2code.mcp.annotated.annotation.McpPrompt;
import com.github.thought2code.mcp.annotated.annotation.McpPromptCompletion;
import com.github.thought2code.mcp.annotated.annotation.McpPromptParam;
import com.github.thought2code.mcp.annotated.annotation.McpResource;
import com.github.thought2code.mcp.annotated.annotation.McpResourceCompletion;
import com.github.thought2code.mcp.annotated.annotation.McpTool;
import com.github.thought2code.mcp.annotated.annotation.McpToolParam;
import com.github.thought2code.mcp.annotated.component.completion.McpCompleteCompletion;
import com.github.thought2code.mcp.annotated.util.StringHelper;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;

/**
 * Annotation processor that compiles {@code @McpTool} runtime reflection metadata into static model
 * classes.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@SupportedAnnotationTypes({
  "com.github.thought2code.mcp.annotated.annotation.McpTool",
  "com.github.thought2code.mcp.annotated.annotation.McpPrompt",
  "com.github.thought2code.mcp.annotated.annotation.McpResource",
  "com.github.thought2code.mcp.annotated.annotation.McpPromptCompletion",
  "com.github.thought2code.mcp.annotated.annotation.McpResourceCompletion"
})
public final class McpToolModelProcessor extends AbstractProcessor {

  private static final String GENERATED_PACKAGE = "com.github.thought2code.mcp.annotated.generated";
  private static final String PROVIDER_INTERFACE =
      "com.github.thought2code.mcp.annotated.component.spi.ComponentModelProvider";
  private static final String PROVIDER_SERVICE_FILE = "META-INF/services/" + PROVIDER_INTERFACE;

  private final List<ExecutableElement> tools = new ArrayList<>();
  private final List<ExecutableElement> prompts = new ArrayList<>();
  private final List<ExecutableElement> resources = new ArrayList<>();
  private final List<ExecutableElement> completions = new ArrayList<>();
  private final Set<String> seenMethodSignatures = new HashSet<>();
  private final Set<String> seenPromptMethodSignatures = new HashSet<>();
  private final Set<String> seenResourceMethodSignatures = new HashSet<>();
  private final Set<String> seenCompletionMethodSignatures = new HashSet<>();
  private boolean generated;

  private Filer filer;
  private Messager messager;

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    this.filer = processingEnv.getFiler();
    this.messager = processingEnv.getMessager();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    collectTools(roundEnv);
    collectPrompts(roundEnv);
    collectResources(roundEnv);
    collectCompletions(roundEnv);
    if (!roundEnv.processingOver()
        || generated
        || (tools.isEmpty() && prompts.isEmpty() && resources.isEmpty() && completions.isEmpty())) {
      return false;
    }
    if (!validateNoDuplicateToolNames()
        || !validateNoDuplicatePromptNames()
        || !validateNoDuplicateResourceNames()
        || !validateNoDuplicateCompletionReferences()) {
      return false;
    }
    try {
      writeProvider();
      generated = true;
    } catch (IOException e) {
      messager.printMessage(
          Diagnostic.Kind.ERROR, "Failed to generate MCP tool model: " + e.getMessage());
    }
    return false;
  }

  private void collectTools(RoundEnvironment roundEnv) {
    for (Element element : roundEnv.getElementsAnnotatedWith(McpTool.class)) {
      if (!(element instanceof ExecutableElement method)) {
        continue;
      }
      if (!validateToolMethod(method)) {
        continue;
      }
      String signature = sourceMethod(method);
      if (seenMethodSignatures.add(signature)) {
        tools.add(method);
      }
    }
  }

  private void collectPrompts(RoundEnvironment roundEnv) {
    for (Element element : roundEnv.getElementsAnnotatedWith(McpPrompt.class)) {
      if (!(element instanceof ExecutableElement method)) {
        continue;
      }
      if (!validateComponentMethod(method, "@McpPrompt")) {
        continue;
      }
      String signature = sourceMethod(method);
      if (seenPromptMethodSignatures.add(signature)) {
        prompts.add(method);
      }
    }
  }

  private void collectResources(RoundEnvironment roundEnv) {
    for (Element element : roundEnv.getElementsAnnotatedWith(McpResource.class)) {
      if (!(element instanceof ExecutableElement method)) {
        continue;
      }
      if (!validateComponentMethod(method, "@McpResource")) {
        continue;
      }
      String signature = sourceMethod(method);
      if (seenResourceMethodSignatures.add(signature)) {
        resources.add(method);
      }
    }
  }

  private void collectCompletions(RoundEnvironment roundEnv) {
    for (Element element : roundEnv.getElementsAnnotatedWith(McpPromptCompletion.class)) {
      collectCompletionElement(element);
    }
    for (Element element : roundEnv.getElementsAnnotatedWith(McpResourceCompletion.class)) {
      collectCompletionElement(element);
    }
  }

  private void collectCompletionElement(Element element) {
    if (!(element instanceof ExecutableElement method)) {
      return;
    }
    if (!validateComponentMethod(method, "Completion")) {
      return;
    }
    if (!validateCompletionSignature(method)) {
      return;
    }
    String signature = sourceMethod(method);
    if (seenCompletionMethodSignatures.add(signature)) {
      completions.add(method);
    }
  }

  private boolean validateComponentMethod(ExecutableElement method, String annotationName) {
    Element enclosing = method.getEnclosingElement();
    if (!(enclosing instanceof TypeElement typeElement)) {
      messager.printMessage(
          Diagnostic.Kind.ERROR, annotationName + " must be declared in a class", method);
      return false;
    }
    if (!method.getModifiers().contains(Modifier.PUBLIC)) {
      messager.printMessage(
          Diagnostic.Kind.ERROR, annotationName + " method must be public", method);
      return false;
    }
    if (!typeElement.getModifiers().contains(Modifier.PUBLIC)) {
      messager.printMessage(
          Diagnostic.Kind.ERROR, annotationName + " declaring class must be public", method);
      return false;
    }
    if (typeElement.getNestingKind() == NestingKind.MEMBER
        && !typeElement.getModifiers().contains(Modifier.STATIC)) {
      messager.printMessage(
          Diagnostic.Kind.ERROR,
          annotationName + " declaring class must be top-level or static nested class",
          method);
      return false;
    }
    return true;
  }

  private boolean validateToolMethod(ExecutableElement method) {
    return validateComponentMethod(method, "@McpTool");
  }

  private boolean validateNoDuplicateToolNames() {
    Map<String, String> names = new HashMap<>();
    boolean valid = true;
    for (ExecutableElement method : tools) {
      String name = toolName(method);
      String source = sourceMethod(method);
      String previous = names.putIfAbsent(name, source);
      if (previous != null) {
        messager.printMessage(
            Diagnostic.Kind.ERROR,
            "Duplicate @McpTool name '"
                + name
                + "' found for methods "
                + previous
                + " and "
                + source,
            method);
        valid = false;
      }
    }
    return valid;
  }

  private boolean validateNoDuplicatePromptNames() {
    Map<String, String> names = new HashMap<>();
    boolean valid = true;
    for (ExecutableElement method : prompts) {
      String name = promptName(method);
      String source = sourceMethod(method);
      String previous = names.putIfAbsent(name, source);
      if (previous != null) {
        messager.printMessage(
            Diagnostic.Kind.ERROR,
            "Duplicate @McpPrompt name '"
                + name
                + "' found for methods "
                + previous
                + " and "
                + source,
            method);
        valid = false;
      }
    }
    return valid;
  }

  private boolean validateNoDuplicateResourceNames() {
    Map<String, String> names = new HashMap<>();
    boolean valid = true;
    for (ExecutableElement method : resources) {
      String name = resourceName(method);
      String source = sourceMethod(method);
      String previous = names.putIfAbsent(name, source);
      if (previous != null) {
        messager.printMessage(
            Diagnostic.Kind.ERROR,
            "Duplicate @McpResource name '"
                + name
                + "' found for methods "
                + previous
                + " and "
                + source,
            method);
        valid = false;
      }
    }
    return valid;
  }

  private boolean validateNoDuplicateCompletionReferences() {
    Map<String, String> references = new HashMap<>();
    boolean valid = true;
    for (ExecutableElement method : completions) {
      String reference = completionReferenceKey(method);
      String source = sourceMethod(method);
      String previous = references.putIfAbsent(reference, source);
      if (previous != null) {
        messager.printMessage(
            Diagnostic.Kind.ERROR,
            "Duplicate completion reference '"
                + reference
                + "' found for methods "
                + previous
                + " and "
                + source,
            method);
        valid = false;
      }
    }
    return valid;
  }

  private boolean validateCompletionSignature(ExecutableElement method) {
    String returnType = erasedType(method.getReturnType());
    if (!McpCompleteCompletion.class.getName().equals(returnType)) {
      messager.printMessage(
          Diagnostic.Kind.ERROR, "Completion method must return McpCompleteCompletion", method);
      return false;
    }
    if (method.getParameters().size() != 1) {
      messager.printMessage(
          Diagnostic.Kind.ERROR,
          "Completion method must have exactly one parameter of type McpSchema.CompleteRequest.CompleteArgument",
          method);
      return false;
    }
    String parameterType = erasedType(method.getParameters().get(0).asType());
    if (!"io.modelcontextprotocol.spec.McpSchema.CompleteRequest.CompleteArgument"
        .equals(parameterType)) {
      messager.printMessage(
          Diagnostic.Kind.ERROR,
          "Completion method must have exactly one parameter of type McpSchema.CompleteRequest.CompleteArgument",
          method);
      return false;
    }
    return true;
  }

  private void writeProvider() throws IOException {
    List<ExecutableElement> sortedTools =
        tools.stream().sorted(Comparator.comparing(this::sourceMethod)).toList();
    List<ExecutableElement> sortedPrompts =
        prompts.stream().sorted(Comparator.comparing(this::sourceMethod)).toList();
    List<ExecutableElement> sortedResources =
        resources.stream().sorted(Comparator.comparing(this::sourceMethod)).toList();
    List<ExecutableElement> sortedCompletions =
        completions.stream().sorted(Comparator.comparing(this::sourceMethod)).toList();
    String className =
        "GeneratedMcpModelProvider_"
            + Integer.toHexString(
                modelHash(sortedTools, sortedPrompts, sortedResources, sortedCompletions));
    String qualifiedName = GENERATED_PACKAGE + "." + className;

    try (Writer writer = filer.createSourceFile(qualifiedName).openWriter()) {
      writer.write("package " + GENERATED_PACKAGE + ";\n\n");
      writer.write("import com.github.thought2code.mcp.annotated.McpApplicationContext;\n");
      writer.write(
          "import com.github.thought2code.mcp.annotated.component.completion.CompletionDefinition;\n");
      writer.write(
          "import com.github.thought2code.mcp.annotated.component.completion.CompletionInvoker;\n");
      writer.write(
          "import com.github.thought2code.mcp.annotated.component.prompt.PromptDefinition;\n");
      writer.write(
          "import com.github.thought2code.mcp.annotated.component.prompt.PromptInvoker;\n");
      writer.write(
          "import com.github.thought2code.mcp.annotated.component.resource.ResourceDefinition;\n");
      writer.write(
          "import com.github.thought2code.mcp.annotated.component.resource.ResourceInvoker;\n");
      writer.write(
          "import com.github.thought2code.mcp.annotated.component.spi.ComponentModelProvider;\n");
      writer.write("import com.github.thought2code.mcp.annotated.component.tool.ToolDefinition;\n");
      writer.write("import com.github.thought2code.mcp.annotated.component.tool.ToolInvoker;\n");
      writer.write("import com.github.thought2code.mcp.annotated.enums.McpServerError;\n");
      writer.write("import com.github.thought2code.mcp.annotated.reflect.Invocation;\n");
      writer.write("import com.github.thought2code.mcp.annotated.util.TypeConverter;\n");
      writer.write("import io.modelcontextprotocol.spec.McpSchema;\n");
      writer.write("import java.util.ArrayList;\n");
      writer.write("import java.util.HashMap;\n");
      writer.write("import java.util.LinkedHashMap;\n");
      writer.write("import java.util.List;\n");
      writer.write("import java.util.Map;\n\n");
      writer.write("public final class " + className + " implements ComponentModelProvider {\n\n");

      writer.write("  @Override\n");
      writer.write("  public List<ToolDefinition> tools() {\n");
      writer.write("    List<ToolDefinition> definitions = new ArrayList<>();\n");
      for (int i = 0; i < sortedTools.size(); i++) {
        ExecutableElement method = sortedTools.get(i);
        writer.write("    definitions.add(toolDefinition" + i + "());\n");
      }
      writer.write("    return definitions;\n");
      writer.write("  }\n\n");

      writer.write("  @Override\n");
      writer.write("  public List<ResourceDefinition> resources() {\n");
      writer.write("    List<ResourceDefinition> definitions = new ArrayList<>();\n");
      for (int i = 0; i < sortedResources.size(); i++) {
        writer.write("    definitions.add(resourceDefinition" + i + "());\n");
      }
      writer.write("    return definitions;\n");
      writer.write("  }\n\n");

      writer.write("  @Override\n");
      writer.write("  public List<CompletionDefinition> completions() {\n");
      writer.write("    List<CompletionDefinition> definitions = new ArrayList<>();\n");
      for (int i = 0; i < sortedCompletions.size(); i++) {
        writer.write("    definitions.add(completionDefinition" + i + "());\n");
      }
      writer.write("    return definitions;\n");
      writer.write("  }\n\n");

      writer.write("  @Override\n");
      writer.write("  public List<PromptDefinition> prompts() {\n");
      writer.write("    List<PromptDefinition> definitions = new ArrayList<>();\n");
      for (int i = 0; i < sortedPrompts.size(); i++) {
        writer.write("    definitions.add(promptDefinition" + i + "());\n");
      }
      writer.write("    return definitions;\n");
      writer.write("  }\n\n");

      for (int i = 0; i < sortedTools.size(); i++) {
        writeToolDefinitionMethod(writer, sortedTools.get(i), i);
      }

      for (int i = 0; i < sortedTools.size(); i++) {
        writeInputSchemaMethod(writer, sortedTools.get(i), i);
      }

      for (int i = 0; i < sortedTools.size(); i++) {
        writeOutputSchemaMethod(writer, sortedTools.get(i), i);
      }

      for (int i = 0; i < sortedTools.size(); i++) {
        writeInvoker(writer, sortedTools.get(i), i);
      }

      for (int i = 0; i < sortedPrompts.size(); i++) {
        writePromptDefinitionMethod(writer, sortedPrompts.get(i), i);
      }

      for (int i = 0; i < sortedPrompts.size(); i++) {
        writePromptInvoker(writer, sortedPrompts.get(i), i);
      }

      for (int i = 0; i < sortedResources.size(); i++) {
        writeResourceDefinitionMethod(writer, sortedResources.get(i), i);
      }

      for (int i = 0; i < sortedResources.size(); i++) {
        writeResourceInvoker(writer, sortedResources.get(i), i);
      }

      for (int i = 0; i < sortedCompletions.size(); i++) {
        writeCompletionDefinitionMethod(writer, sortedCompletions.get(i), i);
      }

      for (int i = 0; i < sortedCompletions.size(); i++) {
        writeCompletionInvoker(writer, sortedCompletions.get(i), i);
      }

      writer.write("}\n");
    }

    try (Writer serviceWriter =
        filer
            .createResource(StandardLocation.CLASS_OUTPUT, "", PROVIDER_SERVICE_FILE)
            .openWriter()) {
      serviceWriter.write(qualifiedName);
      serviceWriter.write('\n');
    }
  }

  private void writeToolDefinitionMethod(Writer writer, ExecutableElement method, int index)
      throws IOException {
    String sourceMethod = sourceMethod(method);
    String name = toolName(method);
    String title = toolTitle(method);
    String description = toolDescription(method);

    writer.write("  private static ToolDefinition toolDefinition" + index + "() {\n");
    writer.write("    Map<String, Object> inputSchema = inputSchema" + index + "();\n");
    writer.write("    Map<String, Object> outputSchema = outputSchema" + index + "();\n");
    writer.write(
        "    McpSchema.Tool tool = McpSchema.Tool.builder(\""
            + escape(name)
            + "\", inputSchema)\n");
    writer.write("        .title(\"" + escape(title) + "\")\n");
    writer.write("        .description(\"" + escape(description) + "\")\n");
    writer.write("        .outputSchema(outputSchema)\n");
    writer.write("        .build();\n");
    writer.write(
        "    return new ToolDefinition(\""
            + escape(sourceMethod)
            + "\", tool, new Invoker"
            + index
            + "());\n");
    writer.write("  }\n\n");
  }

  private void writeInputSchemaMethod(Writer writer, ExecutableElement method, int index)
      throws IOException {
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
      String description = StringHelper.defaultIfBlank(toolParam.description(), paramName);
      String javaType = erasedType(parameter.asType());
      TypeElement typeElement = asTypeElement(parameter.asType());

      writer.write("    {\n");
      writer.write("      Map<String, Object> property = new HashMap<>();\n");
      if (typeElement != null && typeElement.getAnnotation(McpJsonSchemaDefinition.class) != null) {
        String definitionName = typeElement.getSimpleName().toString();
        writer.write(
            "      property.put(\"$ref\", \"#/definitions/" + escape(definitionName) + "\");\n");
        writeDefinitionLiteral(writer, "      ", "definitions", definitionName, typeElement);
      } else {
        writer.write(
            "      property.put(\"type\", \"" + escape(toJsonSchemaType(javaType)) + "\");\n");
        writer.write("      property.put(\"description\", \"" + escape(description) + "\");\n");
      }
      writer.write("      properties.put(\"" + escape(paramName) + "\", property);\n");
      if (toolParam.required()) {
        writer.write("      required.add(\"" + escape(paramName) + "\");\n");
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

  private void writeOutputSchemaMethod(Writer writer, ExecutableElement method, int index)
      throws IOException {
    writer.write("  private static Map<String, Object> outputSchema" + index + "() {\n");
    writer.write("    Map<String, Object> schema = new HashMap<>();\n");
    writer.write("    schema.put(\"type\", \"object\");\n");
    writer.write("    Map<String, Object> properties = new LinkedHashMap<>();\n");
    writer.write("    List<String> required = new ArrayList<>();\n");

    TypeElement returnType = asTypeElement(method.getReturnType());
    if (returnType != null) {
      for (PropertySpec property : schemaProperties(returnType)) {
        writer.write("    {\n");
        writer.write("      Map<String, Object> fieldProperties = new HashMap<>();\n");
        writer.write("      fieldProperties.put(\"type\", \"" + escape(property.type()) + "\");\n");
        writer.write(
            "      fieldProperties.put(\"description\", \""
                + escape(property.description())
                + "\");\n");
        writer.write(
            "      properties.put(\"" + escape(property.name()) + "\", fieldProperties);\n");
        if (property.required()) {
          writer.write("      required.add(\"" + escape(property.name()) + "\");\n");
        }
        writer.write("    }\n");
      }
    }

    writer.write("    schema.put(\"properties\", properties);\n");
    writer.write("    schema.put(\"required\", required);\n");
    writer.write("    return schema;\n");
    writer.write("  }\n\n");
  }

  private void writeInvoker(Writer writer, ExecutableElement method, int index) throws IOException {
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
      String paramType = parameterDeclarationType(parameter.asType());
      String targetClassLiteral = classLiteral(parameter.asType());
      McpToolParam toolParam = parameter.getAnnotation(McpToolParam.class);
      String valueExpr =
          toolParam == null
              ? "TypeConverter.convert(null, " + targetClassLiteral + ")"
              : "TypeConverter.convert(safeArguments.get(\""
                  + escape(toolParam.name())
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

  private void writePromptDefinitionMethod(Writer writer, ExecutableElement method, int index)
      throws IOException {
    String sourceMethod = sourceMethod(method);
    String name = promptName(method);
    String title = promptTitle(method);
    String description = promptDescription(method);

    writer.write("  private static PromptDefinition promptDefinition" + index + "() {\n");
    writer.write("    List<McpSchema.PromptArgument> args = new ArrayList<>();\n");
    for (VariableElement parameter : method.getParameters()) {
      McpPromptParam promptParam = parameter.getAnnotation(McpPromptParam.class);
      if (promptParam == null) {
        continue;
      }
      String paramName = promptParam.name();
      String paramTitle = StringHelper.defaultIfBlank(promptParam.title(), paramName);
      String paramDescription = StringHelper.defaultIfBlank(promptParam.description(), paramName);
      writer.write(
          "    args.add(McpSchema.PromptArgument.builder(\""
              + escape(paramName)
              + "\")\n"
              + "        .title(\""
              + escape(paramTitle)
              + "\")\n"
              + "        .description(\""
              + escape(paramDescription)
              + "\")\n"
              + "        .required("
              + promptParam.required()
              + ")\n"
              + "        .build());\n");
    }

    writer.write(
        "    McpSchema.Prompt prompt = McpSchema.Prompt.builder(\""
            + escape(name)
            + "\")\n"
            + "        .title(\""
            + escape(title)
            + "\")\n"
            + "        .description(\""
            + escape(description)
            + "\")\n"
            + "        .arguments(args)\n"
            + "        .build();\n");
    writer.write(
        "    return new PromptDefinition(\""
            + escape(sourceMethod)
            + "\", prompt, \""
            + escape(description)
            + "\", new PromptInvoker"
            + index
            + "());\n");
    writer.write("  }\n\n");
  }

  private void writePromptInvoker(Writer writer, ExecutableElement method, int index)
      throws IOException {
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
      String paramType = parameterDeclarationType(parameter.asType());
      String targetClassLiteral = classLiteral(parameter.asType());
      McpPromptParam promptParam = parameter.getAnnotation(McpPromptParam.class);
      String valueExpr =
          promptParam == null
              ? "TypeConverter.convert(null, " + targetClassLiteral + ")"
              : "TypeConverter.convert(safeArguments.get(\""
                  + escape(promptParam.name())
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

  private void writeResourceDefinitionMethod(Writer writer, ExecutableElement method, int index)
      throws IOException {
    String sourceMethod = sourceMethod(method);
    String uri = resourceUri(method);
    String name = resourceName(method);
    String title = resourceTitle(method);
    String description = resourceDescription(method);
    String mimeType = resourceMimeType(method);
    String rolesLiteral = resourceRolesLiteral(method);
    double priority = resourcePriority(method);

    writer.write("  private static ResourceDefinition resourceDefinition" + index + "() {\n");
    writer.write(
        "    McpSchema.Resource resource = McpSchema.Resource.builder(\""
            + escape(uri)
            + "\", \""
            + escape(name)
            + "\")\n");
    writer.write("        .title(\"" + escape(title) + "\")\n");
    writer.write("        .description(\"" + escape(description) + "\")\n");
    writer.write("        .mimeType(\"" + escape(mimeType) + "\")\n");
    writer.write(
        "        .annotations(McpSchema.Annotations.builder().audience("
            + rolesLiteral
            + ").priority("
            + priority
            + ").build())\n");
    writer.write("        .build();\n");
    writer.write(
        "    return new ResourceDefinition(\""
            + escape(sourceMethod)
            + "\", resource, new ResourceInvoker"
            + index
            + "());\n");
    writer.write("  }\n\n");
  }

  private void writeResourceInvoker(Writer writer, ExecutableElement method, int index)
      throws IOException {
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
        "        return Invocation.builder().result(McpServerError.METHOD_INVOCATION_ERROR.toString()).isError(true).build();\n");
    writer.write("      }\n");
    writer.write("    }\n");
    writer.write("  }\n\n");
  }

  private void writeCompletionDefinitionMethod(Writer writer, ExecutableElement method, int index)
      throws IOException {
    String sourceMethod = sourceMethod(method);
    writer.write("  private static CompletionDefinition completionDefinition" + index + "() {\n");
    if (method.getAnnotation(McpPromptCompletion.class) != null) {
      McpPromptCompletion annotation = method.getAnnotation(McpPromptCompletion.class);
      writer.write(
          "    McpSchema.CompleteReference reference = McpSchema.PromptReference.builder(\""
              + escape(annotation.name())
              + "\")");
      if (!StringHelper.isBlank(annotation.title())) {
        writer.write(".title(\"" + escape(annotation.title()) + "\")");
      }
      writer.write(".build();\n");
    } else {
      McpResourceCompletion annotation = method.getAnnotation(McpResourceCompletion.class);
      writer.write(
          "    McpSchema.CompleteReference reference = new McpSchema.ResourceReference(\""
              + escape(annotation.uri())
              + "\");\n");
    }
    writer.write(
        "    return new CompletionDefinition(\""
            + escape(sourceMethod)
            + "\", reference, new CompletionInvoker"
            + index
            + "());\n");
    writer.write("  }\n\n");
  }

  private void writeCompletionInvoker(Writer writer, ExecutableElement method, int index)
      throws IOException {
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
        "        return Invocation.builder().result(McpServerError.METHOD_INVOCATION_ERROR.toString()).isError(true).build();\n");
    writer.write("      }\n");
    writer.write("    }\n");
    writer.write("  }\n\n");
  }

  private void writeDefinitionLiteral(
      Writer writer,
      String indent,
      String targetMap,
      String definitionName,
      TypeElement definitionType)
      throws IOException {
    writer.write(indent + "{\n");
    writer.write(indent + "  Map<String, Object> definition = new HashMap<>();\n");
    writer.write(indent + "  definition.put(\"type\", \"object\");\n");
    writer.write(indent + "  Map<String, Object> definitionProperties = new LinkedHashMap<>();\n");
    writer.write(indent + "  List<String> definitionRequired = new ArrayList<>();\n");
    for (PropertySpec property : schemaProperties(definitionType)) {
      writer.write(indent + "  {\n");
      writer.write(indent + "    Map<String, Object> fieldProperties = new HashMap<>();\n");
      writer.write(
          indent + "    fieldProperties.put(\"type\", \"" + escape(property.type()) + "\");\n");
      writer.write(
          indent
              + "    fieldProperties.put(\"description\", \""
              + escape(property.description())
              + "\");\n");
      writer.write(
          indent
              + "    definitionProperties.put(\""
              + escape(property.name())
              + "\", fieldProperties);\n");
      if (property.required()) {
        writer.write(indent + "    definitionRequired.add(\"" + escape(property.name()) + "\");\n");
      }
      writer.write(indent + "  }\n");
    }
    writer.write(indent + "  definition.put(\"properties\", definitionProperties);\n");
    writer.write(indent + "  definition.put(\"required\", definitionRequired);\n");
    writer.write(
        indent + "  " + targetMap + ".put(\"" + escape(definitionName) + "\", definition);\n");
    writer.write(indent + "}\n");
  }

  private List<PropertySpec> schemaProperties(TypeElement type) {
    Map<String, PropertySpec> properties = new LinkedHashMap<>();
    for (Element element : type.getEnclosedElements()) {
      if (element.getKind() == ElementKind.FIELD) {
        McpJsonSchemaProperty property = element.getAnnotation(McpJsonSchemaProperty.class);
        if (property != null) {
          Name fieldName = element.getSimpleName();
          String propertyName = StringHelper.defaultIfBlank(property.name(), fieldName.toString());
          String description = StringHelper.defaultIfBlank(property.description(), propertyName);
          String javaType = erasedType(element.asType());
          properties.put(
              propertyName,
              new PropertySpec(
                  propertyName, toJsonSchemaType(javaType), description, property.required()));
        }
      } else if (element.getKind() == ElementKind.RECORD_COMPONENT) {
        RecordComponentElement recordComponent = (RecordComponentElement) element;
        McpJsonSchemaProperty property = recordComponent.getAnnotation(McpJsonSchemaProperty.class);
        if (property != null) {
          Name fieldName = recordComponent.getSimpleName();
          String propertyName = StringHelper.defaultIfBlank(property.name(), fieldName.toString());
          String description = StringHelper.defaultIfBlank(property.description(), propertyName);
          String javaType = erasedType(recordComponent.asType());
          properties.putIfAbsent(
              propertyName,
              new PropertySpec(
                  propertyName, toJsonSchemaType(javaType), description, property.required()));
        }
      }
    }
    return new ArrayList<>(properties.values());
  }

  private String sourceMethod(ExecutableElement method) {
    TypeElement owner = (TypeElement) method.getEnclosingElement();
    return owner.getQualifiedName() + "#" + method;
  }

  private String toolName(ExecutableElement method) {
    McpTool annotation = method.getAnnotation(McpTool.class);
    String defaultName = StringHelper.toSnakeCase(method.getSimpleName().toString());
    return StringHelper.defaultIfBlank(annotation.name(), defaultName);
  }

  private String toolTitle(ExecutableElement method) {
    McpTool annotation = method.getAnnotation(McpTool.class);
    String name = toolName(method);
    return StringHelper.defaultIfBlank(annotation.title(), name);
  }

  private String toolDescription(ExecutableElement method) {
    McpTool annotation = method.getAnnotation(McpTool.class);
    String name = toolName(method);
    return StringHelper.defaultIfBlank(annotation.description(), name);
  }

  private String promptName(ExecutableElement method) {
    McpPrompt annotation = method.getAnnotation(McpPrompt.class);
    String defaultName = StringHelper.toSnakeCase(method.getSimpleName().toString());
    return StringHelper.defaultIfBlank(annotation.name(), defaultName);
  }

  private String promptTitle(ExecutableElement method) {
    McpPrompt annotation = method.getAnnotation(McpPrompt.class);
    String name = promptName(method);
    return StringHelper.defaultIfBlank(annotation.title(), name);
  }

  private String promptDescription(ExecutableElement method) {
    McpPrompt annotation = method.getAnnotation(McpPrompt.class);
    String name = promptName(method);
    return StringHelper.defaultIfBlank(annotation.description(), name);
  }

  private String resourceUri(ExecutableElement method) {
    return method.getAnnotation(McpResource.class).uri();
  }

  private String resourceName(ExecutableElement method) {
    McpResource annotation = method.getAnnotation(McpResource.class);
    String defaultName = StringHelper.toSnakeCase(method.getSimpleName().toString());
    return StringHelper.defaultIfBlank(annotation.name(), defaultName);
  }

  private String resourceTitle(ExecutableElement method) {
    McpResource annotation = method.getAnnotation(McpResource.class);
    String name = resourceName(method);
    return StringHelper.defaultIfBlank(annotation.title(), name);
  }

  private String resourceDescription(ExecutableElement method) {
    McpResource annotation = method.getAnnotation(McpResource.class);
    String name = resourceName(method);
    return StringHelper.defaultIfBlank(annotation.description(), name);
  }

  private String resourceMimeType(ExecutableElement method) {
    return method.getAnnotation(McpResource.class).mimeType();
  }

  private double resourcePriority(ExecutableElement method) {
    return method.getAnnotation(McpResource.class).priority();
  }

  private String resourceRolesLiteral(ExecutableElement method) {
    var roles = method.getAnnotation(McpResource.class).roles();
    if (roles.length == 0) {
      return "List.of()";
    }
    List<String> parts = new ArrayList<>(roles.length);
    for (var role : roles) {
      parts.add("McpSchema.Role." + role.name());
    }
    return "List.of(" + String.join(", ", parts) + ")";
  }

  private String completionReferenceKey(ExecutableElement method) {
    McpPromptCompletion prompt = method.getAnnotation(McpPromptCompletion.class);
    if (prompt != null) {
      return "prompt:" + prompt.name();
    }
    McpResourceCompletion resource = method.getAnnotation(McpResourceCompletion.class);
    if (resource != null) {
      return "resource:" + resource.uri();
    }
    return "unknown:" + sourceMethod(method);
  }

  private int modelHash(
      List<ExecutableElement> toolsToHash,
      List<ExecutableElement> promptsToHash,
      List<ExecutableElement> resourcesToHash,
      List<ExecutableElement> completionsToHash) {
    int hash = 17;
    for (ExecutableElement method : toolsToHash) {
      hash = 31 * hash + sourceMethod(method).hashCode();
      hash = 31 * hash + toolName(method).hashCode();
    }
    for (ExecutableElement method : promptsToHash) {
      hash = 31 * hash + sourceMethod(method).hashCode();
      hash = 31 * hash + promptName(method).hashCode();
    }
    for (ExecutableElement method : resourcesToHash) {
      hash = 31 * hash + sourceMethod(method).hashCode();
      hash = 31 * hash + resourceName(method).hashCode();
      hash = 31 * hash + resourceUri(method).hashCode();
    }
    for (ExecutableElement method : completionsToHash) {
      hash = 31 * hash + sourceMethod(method).hashCode();
      hash = 31 * hash + completionReferenceKey(method).hashCode();
    }
    return hash;
  }

  private String erasedType(TypeMirror mirror) {
    if (mirror.getKind().isPrimitive()) {
      return mirror.toString();
    }
    return processingEnv.getTypeUtils().erasure(mirror).toString();
  }

  private String parameterDeclarationType(TypeMirror mirror) {
    return switch (mirror.getKind()) {
      case BYTE -> "java.lang.Byte";
      case SHORT -> "java.lang.Short";
      case CHAR -> "java.lang.Character";
      case INT -> "java.lang.Integer";
      case LONG -> "java.lang.Long";
      case FLOAT -> "java.lang.Float";
      case DOUBLE -> "java.lang.Double";
      case BOOLEAN -> "java.lang.Boolean";
      default -> erasedType(mirror);
    };
  }

  private String classLiteral(TypeMirror mirror) {
    if (mirror.getKind().isPrimitive()) {
      return mirror + ".class";
    }
    return erasedType(mirror) + ".class";
  }

  private TypeElement asTypeElement(TypeMirror mirror) {
    if (mirror.getKind().isPrimitive()) {
      return null;
    }
    Element element = processingEnv.getTypeUtils().asElement(mirror);
    return element instanceof TypeElement typeElement ? typeElement : null;
  }

  private String toJsonSchemaType(String javaType) {
    return switch (javaType) {
      case "java.lang.String" -> "string";
      case "java.lang.Object" -> "object";
      case "byte", "java.lang.Byte", "short", "java.lang.Short", "int", "java.lang.Integer" ->
          "integer";
      case "char", "java.lang.Character" -> "string";
      case "long",
          "java.lang.Long",
          "float",
          "java.lang.Float",
          "double",
          "java.lang.Double",
          "java.lang.Number" ->
          "number";
      case "boolean", "java.lang.Boolean" -> "boolean";
      default -> "string";
    };
  }

  private String escape(String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  private record PropertySpec(String name, String type, String description, boolean required) {}
}
