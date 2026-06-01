package com.github.thought2code.mcp.annotated.server.component;

import com.github.thought2code.mcp.annotated.annotation.McpJsonSchemaProperty;
import com.github.thought2code.mcp.annotated.annotation.McpPrompt;
import com.github.thought2code.mcp.annotated.annotation.McpPromptCompletion;
import com.github.thought2code.mcp.annotated.annotation.McpResource;
import com.github.thought2code.mcp.annotated.annotation.McpResourceCompletion;
import com.github.thought2code.mcp.annotated.annotation.McpTool;
import com.github.thought2code.mcp.annotated.server.component.completion.CompletionCodegen;
import com.github.thought2code.mcp.annotated.server.component.completion.CompletionResult;
import com.github.thought2code.mcp.annotated.server.component.prompt.PromptCodegen;
import com.github.thought2code.mcp.annotated.server.component.resource.ResourceCodegen;
import com.github.thought2code.mcp.annotated.server.component.tool.ToolCodegen;
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
import java.util.Objects;
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
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;

/**
 * Annotation processor that compiles MCP component annotations into static classes at build time.
 *
 * <p>Handles {@code @McpTool}, {@code @McpPrompt}, {@code @McpResource},
 * {@code @McpPromptCompletion}, and {@code @McpResourceCompletion}.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@SupportedAnnotationTypes({
  "com.github.thought2code.mcp.annotated.annotation.McpTool",
  "com.github.thought2code.mcp.annotated.annotation.McpPrompt",
  "com.github.thought2code.mcp.annotated.annotation.McpResource",
  "com.github.thought2code.mcp.annotated.annotation.McpPromptCompletion",
  "com.github.thought2code.mcp.annotated.annotation.McpResourceCompletion"
})
public final class AnnotationProcessor extends AbstractProcessor {

  private static final String GENERATED_PACKAGE = "com.github.thought2code.mcp.annotated.generated";
  private static final String PROVIDER_INTERFACE =
      "com.github.thought2code.mcp.annotated.server.component.ComponentProvider";
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
  private final ToolCodegen.Support toolCodegenSupport = new ToolCodegenSupport(this);
  private final PromptCodegen.Support promptCodegenSupport = new PromptCodegenSupport(this);
  private final ResourceCodegen.Support resourceCodegenSupport = new ResourceCodegenSupport(this);
  private final CompletionCodegen.Support completionCodegenSupport =
      new CompletionCodegenSupport(this);

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
          Diagnostic.Kind.ERROR, "Failed to generate MCP component class: " + e.getMessage());
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
            DuplicateComponentMessageHelper.duplicateToolName(name, previous, source),
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
            DuplicateComponentMessageHelper.duplicatePromptName(name, previous, source),
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
            DuplicateComponentMessageHelper.duplicateResourceName(name, previous, source),
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
      String referenceDescription = completionReferenceDescription(method);
      String source = sourceMethod(method);
      String previous = references.putIfAbsent(reference, source);
      if (previous != null) {
        messager.printMessage(
            Diagnostic.Kind.ERROR,
            DuplicateComponentMessageHelper.duplicateCompletionReference(
                referenceDescription, previous, source),
            method);
        valid = false;
      }
    }
    return valid;
  }

  private boolean validateCompletionSignature(ExecutableElement method) {
    String returnType = erasedType(method.getReturnType());
    if (!CompletionResult.class.getName().equals(returnType)) {
      messager.printMessage(
          Diagnostic.Kind.ERROR, "Completion method must return CompletionResult", method);
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
        "GeneratedComponentProvider_"
            + Integer.toHexString(
                componentHash(sortedTools, sortedPrompts, sortedResources, sortedCompletions));
    String qualifiedName = GENERATED_PACKAGE + "." + className;

    try (Writer writer = filer.createSourceFile(qualifiedName).openWriter()) {
      writer.write("package " + GENERATED_PACKAGE + ";\n\n");
      writer.write("import com.github.thought2code.mcp.annotated.McpApplicationContext;\n");
      writer.write(
          "import com.github.thought2code.mcp.annotated.server.component.completion.CompletionDefinition;\n");
      writer.write(
          "import com.github.thought2code.mcp.annotated.server.component.completion.CompletionInvoker;\n");
      writer.write(
          "import com.github.thought2code.mcp.annotated.server.component.prompt.PromptDefinition;\n");
      writer.write(
          "import com.github.thought2code.mcp.annotated.server.component.prompt.PromptInvoker;\n");
      writer.write(
          "import com.github.thought2code.mcp.annotated.server.component.resource.ResourceDefinition;\n");
      writer.write(
          "import com.github.thought2code.mcp.annotated.server.component.resource.ResourceInvoker;\n");
      writer.write(
          "import com.github.thought2code.mcp.annotated.server.component.ComponentProvider;\n");
      writer.write(
          "import com.github.thought2code.mcp.annotated.server.component.InvocationLogMessageHelper;\n");
      writer.write(
          "import com.github.thought2code.mcp.annotated.server.component.tool.ToolDefinition;\n");
      writer.write(
          "import com.github.thought2code.mcp.annotated.server.component.tool.ToolInvoker;\n");
      writer.write("import com.github.thought2code.mcp.annotated.enums.McpServerError;\n");
      writer.write("import com.github.thought2code.mcp.annotated.server.component.Invocation;\n");
      writer.write("import com.github.thought2code.mcp.annotated.util.TypeConverter;\n");
      writer.write("import io.modelcontextprotocol.spec.McpSchema;\n");
      writer.write("import java.util.ArrayList;\n");
      writer.write("import java.util.HashMap;\n");
      writer.write("import java.util.LinkedHashMap;\n");
      writer.write("import java.util.List;\n");
      writer.write("import java.util.Map;\n\n");
      writer.write("import org.slf4j.Logger;\n");
      writer.write("import org.slf4j.LoggerFactory;\n\n");
      writer.write("public final class " + className + " implements ComponentProvider {\n\n");
      writer.write(
          "  private static final Logger log = LoggerFactory.getLogger("
              + className
              + ".class);\n\n");

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

      ToolCodegen.writeSections(writer, sortedTools, toolCodegenSupport);
      PromptCodegen.writeSections(writer, sortedPrompts, promptCodegenSupport);
      ResourceCodegen.writeSections(writer, sortedResources, resourceCodegenSupport);
      CompletionCodegen.writeSections(writer, sortedCompletions, completionCodegenSupport);

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
    return owner.getQualifiedName() + StringHelper.HASH + method;
  }

  private String toolName(ExecutableElement method) {
    McpTool annotation = Objects.requireNonNull(method.getAnnotation(McpTool.class));
    String defaultName = StringHelper.toSnakeCase(method.getSimpleName().toString());
    return StringHelper.defaultIfBlank(annotation.name(), defaultName);
  }

  private String toolTitle(ExecutableElement method) {
    McpTool annotation = Objects.requireNonNull(method.getAnnotation(McpTool.class));
    String name = toolName(method);
    return StringHelper.defaultIfBlank(annotation.title(), name);
  }

  private String toolDescription(ExecutableElement method) {
    McpTool annotation = Objects.requireNonNull(method.getAnnotation(McpTool.class));
    String name = toolName(method);
    return StringHelper.defaultIfBlank(annotation.description(), name);
  }

  private String promptName(ExecutableElement method) {
    McpPrompt annotation = Objects.requireNonNull(method.getAnnotation(McpPrompt.class));
    String defaultName = StringHelper.toSnakeCase(method.getSimpleName().toString());
    return StringHelper.defaultIfBlank(annotation.name(), defaultName);
  }

  private String promptTitle(ExecutableElement method) {
    McpPrompt annotation = Objects.requireNonNull(method.getAnnotation(McpPrompt.class));
    String name = promptName(method);
    return StringHelper.defaultIfBlank(annotation.title(), name);
  }

  private String promptDescription(ExecutableElement method) {
    McpPrompt annotation = Objects.requireNonNull(method.getAnnotation(McpPrompt.class));
    String name = promptName(method);
    return StringHelper.defaultIfBlank(annotation.description(), name);
  }

  private String resourceUri(ExecutableElement method) {
    return Objects.requireNonNull(method.getAnnotation(McpResource.class)).uri();
  }

  private String resourceName(ExecutableElement method) {
    McpResource annotation = Objects.requireNonNull(method.getAnnotation(McpResource.class));
    String defaultName = StringHelper.toSnakeCase(method.getSimpleName().toString());
    return StringHelper.defaultIfBlank(annotation.name(), defaultName);
  }

  private String resourceTitle(ExecutableElement method) {
    McpResource annotation = Objects.requireNonNull(method.getAnnotation(McpResource.class));
    String name = resourceName(method);
    return StringHelper.defaultIfBlank(annotation.title(), name);
  }

  private String resourceDescription(ExecutableElement method) {
    McpResource annotation = Objects.requireNonNull(method.getAnnotation(McpResource.class));
    String name = resourceName(method);
    return StringHelper.defaultIfBlank(annotation.description(), name);
  }

  private String resourceMimeType(ExecutableElement method) {
    return Objects.requireNonNull(method.getAnnotation(McpResource.class)).mimeType();
  }

  private double resourcePriority(ExecutableElement method) {
    return Objects.requireNonNull(method.getAnnotation(McpResource.class)).priority();
  }

  private String resourceRolesLiteral(ExecutableElement method) {
    var roles = Objects.requireNonNull(method.getAnnotation(McpResource.class)).roles();
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

  private String completionReferenceDescription(ExecutableElement method) {
    McpPromptCompletion prompt = method.getAnnotation(McpPromptCompletion.class);
    if (prompt != null) {
      return DuplicateComponentMessageHelper.completionPromptReferenceDescription(prompt.name());
    }
    McpResourceCompletion resource = method.getAnnotation(McpResourceCompletion.class);
    if (resource != null) {
      return DuplicateComponentMessageHelper.completionResourceReferenceDescription(resource.uri());
    }
    return "'" + completionReferenceKey(method) + "'";
  }

  private int componentHash(
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

  private record ToolCodegenSupport(AnnotationProcessor processor) implements ToolCodegen.Support {

    @Override
    public String sourceMethod(ExecutableElement method) {
      return processor.sourceMethod(method);
    }

    @Override
    public String toolName(ExecutableElement method) {
      return processor.toolName(method);
    }

    @Override
    public String toolTitle(ExecutableElement method) {
      return processor.toolTitle(method);
    }

    @Override
    public String toolDescription(ExecutableElement method) {
      return processor.toolDescription(method);
    }

    @Override
    public String escape(String value) {
      return processor.escape(value);
    }

    @Override
    public String erasedType(TypeMirror mirror) {
      return processor.erasedType(mirror);
    }

    @Override
    public TypeElement asTypeElement(TypeMirror mirror) {
      return processor.asTypeElement(mirror);
    }

    @Override
    public String toJsonSchemaType(String javaType) {
      return processor.toJsonSchemaType(javaType);
    }

    @Override
    public void writeDefinitionLiteral(
        Writer writer,
        String indent,
        String targetMap,
        String definitionName,
        TypeElement definitionType)
        throws IOException {
      processor.writeDefinitionLiteral(writer, indent, targetMap, definitionName, definitionType);
    }

    @Override
    public List<ToolCodegen.PropertySpec> schemaProperties(TypeElement type) {
      List<PropertySpec> properties = processor.schemaProperties(type);
      List<ToolCodegen.PropertySpec> specs = new ArrayList<>(properties.size());
      for (PropertySpec property : properties) {
        specs.add(
            new ToolCodegen.PropertySpec(
                property.name(), property.type(), property.description(), property.required()));
      }
      return specs;
    }

    @Override
    public String parameterDeclarationType(TypeMirror mirror) {
      return processor.parameterDeclarationType(mirror);
    }

    @Override
    public String classLiteral(TypeMirror mirror) {
      return processor.classLiteral(mirror);
    }
  }

  private record PromptCodegenSupport(AnnotationProcessor processor)
      implements PromptCodegen.Support {

    @Override
    public String sourceMethod(ExecutableElement method) {
      return processor.sourceMethod(method);
    }

    @Override
    public String promptName(ExecutableElement method) {
      return processor.promptName(method);
    }

    @Override
    public String promptTitle(ExecutableElement method) {
      return processor.promptTitle(method);
    }

    @Override
    public String promptDescription(ExecutableElement method) {
      return processor.promptDescription(method);
    }

    @Override
    public String escape(String value) {
      return processor.escape(value);
    }

    @Override
    public String parameterDeclarationType(TypeMirror mirror) {
      return processor.parameterDeclarationType(mirror);
    }

    @Override
    public String classLiteral(TypeMirror mirror) {
      return processor.classLiteral(mirror);
    }
  }

  private record ResourceCodegenSupport(AnnotationProcessor processor)
      implements ResourceCodegen.Support {

    @Override
    public String sourceMethod(ExecutableElement method) {
      return processor.sourceMethod(method);
    }

    @Override
    public String resourceUri(ExecutableElement method) {
      return processor.resourceUri(method);
    }

    @Override
    public String resourceName(ExecutableElement method) {
      return processor.resourceName(method);
    }

    @Override
    public String resourceTitle(ExecutableElement method) {
      return processor.resourceTitle(method);
    }

    @Override
    public String resourceDescription(ExecutableElement method) {
      return processor.resourceDescription(method);
    }

    @Override
    public String resourceMimeType(ExecutableElement method) {
      return processor.resourceMimeType(method);
    }

    @Override
    public String resourceRolesLiteral(ExecutableElement method) {
      return processor.resourceRolesLiteral(method);
    }

    @Override
    public double resourcePriority(ExecutableElement method) {
      return processor.resourcePriority(method);
    }

    @Override
    public String escape(String value) {
      return processor.escape(value);
    }
  }

  private record CompletionCodegenSupport(AnnotationProcessor processor)
      implements CompletionCodegen.Support {

    @Override
    public String sourceMethod(ExecutableElement method) {
      return processor.sourceMethod(method);
    }

    @Override
    public String escape(String value) {
      return processor.escape(value);
    }
  }

  private record PropertySpec(String name, String type, String description, boolean required) {}
}
