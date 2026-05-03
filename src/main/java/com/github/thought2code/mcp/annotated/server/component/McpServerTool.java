package com.github.thought2code.mcp.annotated.server.component;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.annotation.McpJsonSchemaDefinition;
import com.github.thought2code.mcp.annotated.annotation.McpJsonSchemaProperty;
import com.github.thought2code.mcp.annotated.annotation.McpTool;
import com.github.thought2code.mcp.annotated.annotation.McpToolParam;
import com.github.thought2code.mcp.annotated.enums.JavaTypeToJsonSchemaMapper;
import com.github.thought2code.mcp.annotated.reflect.Invocation;
import com.github.thought2code.mcp.annotated.reflect.MethodCache;
import com.github.thought2code.mcp.annotated.reflect.MethodInvoker;
import com.github.thought2code.mcp.annotated.server.McpStructuredContent;
import com.github.thought2code.mcp.annotated.server.converter.McpToolParameterConverter;
import com.github.thought2code.mcp.annotated.util.JacksonHelper;
import com.github.thought2code.mcp.annotated.util.StringHelper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MCP server component for handling tool-related operations.
 *
 * <p>This class implements the functionality for creating and registering tool components with an
 * MCP server. It processes methods annotated with {@link McpTool} and creates appropriate tool
 * specifications that can be used to execute operations for LLM interactions.
 *
 * <p>The class handles:
 *
 * <ul>
 *   <li>Creation of tool specifications from annotated methods
 *   <li>Registration of all tool components with the server
 *   <li>Invocation of tool methods with proper argument conversion
 *   <li>JSON schema generation for input parameters and output types
 *   <li>Support for both text and structured content responses
 *   <li>Localization of tool attributes using resource bundles
 * </ul>
 *
 * @author codeboyzhou
 * @see McpTool
 * @see McpToolParam
 * @see McpJsonSchemaDefinition
 * @see McpJsonSchemaProperty
 * @see McpSchema.Tool
 * @see McpSchema.JsonSchema
 * @see McpStructuredContent
 */
public class McpServerTool
    implements McpServerComponent<McpServerFeatures.SyncToolSpecification>, McpComponentRegistrar {

  private static final Logger log = LoggerFactory.getLogger(McpServerTool.class);

  /** The parameter converter for MCP tool parameters. */
  private final McpToolParameterConverter parameterConverter;

  /** Constructor that initializes the tool parameter converter. */
  public McpServerTool() {
    this.parameterConverter = new McpToolParameterConverter();
  }

  /**
   * Creates a synchronous tool specification from the specified method.
   *
   * <p>This method processes a method annotated with {@link McpTool} and creates a {@link
   * McpServerFeatures.SyncToolSpecification} that can be registered with the MCP server. The method
   * extracts tool information from annotations and method signature, generates JSON schemas for
   * input parameters and output types, and creates a tool specification with appropriate metadata.
   *
   * @param method the method annotated with {@link McpTool} to create a specification from
   * @param context the application context for component discovery and localization
   * @return a synchronous tool specification for the MCP server
   * @see McpTool
   * @see McpSchema.Tool
   * @see McpSchema.JsonSchema
   */
  @Override
  public McpServerFeatures.SyncToolSpecification from(
      Method method, McpApplicationContext context) {
    log.info(
        "Creating tool specification for method: {}.{}",
        method.getDeclaringClass().getSimpleName(),
        method.getName());

    // Use reflection cache for performance optimization
    MethodCache methodCache = MethodCache.of(method);
    Object instance = context.getComponentInstance(methodCache.getDeclaringClass());

    McpTool toolMethod = methodCache.getMcpToolAnnotation();
    final String name = StringHelper.defaultIfBlank(toolMethod.name(), methodCache.getMethodName());
    final String title = context.getLocalizedString(toolMethod.title(), name);
    final String description = context.getLocalizedString(toolMethod.description(), name);

    McpSchema.JsonSchema inputSchema = createJsonSchema(context, methodCache.getParameters());
    Map<String, Object> outputSchema =
        createJsonSchemaDefinition(context, methodCache.getReturnType());
    McpSchema.Tool tool =
        McpSchema.Tool.builder()
            .name(name)
            .title(title)
            .description(description)
            .inputSchema(inputSchema)
            .outputSchema(outputSchema)
            .build();

    log.info("Tool specification created: {}", JacksonHelper.toJsonString(tool));

    return McpServerFeatures.SyncToolSpecification.builder()
        .tool(tool)
        .callHandler((exchange, request) -> invoke(instance, methodCache, request))
        .build();
  }

  /**
   * Registers all discovered components of this type with the given MCP server.
   *
   * <p>This method scans for methods annotated with the appropriate annotation(s) for this
   * component type and registers them with the server. The exact discovery and registration
   * mechanism depends on the implementation.
   *
   * @param server the {@link McpSyncServer} instance to register the components with
   * @param context the application context for component discovery and localization
   */
  @Override
  public void register(McpSyncServer server, McpApplicationContext context) {
    Set<Method> methods = context.getMethodsAnnotatedWith(McpTool.class);
    methods.forEach(
        method -> {
          log.debug("Registering tool method: {}", method.toGenericString());
          McpServerFeatures.SyncToolSpecification tool = from(method, context);
          server.addTool(tool);
          log.debug("Tool {} registered successfully", tool.tool().name());
        });
  }

  /**
   * Invokes the tool method with the specified arguments and request.
   *
   * <p>This private method handles the actual invocation of the tool method, converting request
   * arguments to the appropriate parameter types and invoking the method using reflection. The
   * result is then wrapped in a {@link McpSchema.CallToolResult} with both text content and
   * structured content support.
   *
   * @param instance the object instance containing the tool method
   * @param methodCache the cached method information for efficient invocation
   * @param request the tool request containing the arguments
   * @return the result of the tool invocation
   * @see McpSchema.CallToolResult
   * @see McpSchema.TextContent
   * @see McpStructuredContent
   */
  private McpSchema.CallToolResult invoke(
      Object instance, MethodCache methodCache, McpSchema.CallToolRequest request) {

    log.debug("Handling MCP CallToolRequest: {}", JacksonHelper.toJsonString(request));

    Map<String, Object> arguments = request.arguments();
    List<Object> params = parameterConverter.convertAll(methodCache.getParameters(), arguments);
    Invocation invocation = MethodInvoker.invoke(instance, methodCache, params);

    Object result = invocation.result();
    String textContent = result.toString();
    Object structuredContent = Map.of();

    if (result instanceof McpStructuredContent mcpStructuredContent) {
      textContent = mcpStructuredContent.asTextContent();
      structuredContent = mcpStructuredContent;
    }

    McpSchema.CallToolResult callToolResult =
        McpSchema.CallToolResult.builder()
            .content(List.of(new McpSchema.TextContent(textContent)))
            .structuredContent(structuredContent)
            .isError(invocation.isError())
            .build();

    log.debug("Returning MCP CallToolResult: {}", JacksonHelper.toJsonString(callToolResult));

    return callToolResult;
  }

  /**
   * Creates a JSON schema for the tool method parameters.
   *
   * <p>This private method processes method parameters and creates a JSON schema that describes the
   * input parameters for a tool. It handles both primitive types and complex types annotated with
   * {@link McpJsonSchemaDefinition}.
   *
   * @param context the application context for localization
   * @param methodParams the array of method parameters to create a schema for
   * @return a JSON schema describing the tool's input parameters
   * @see McpToolParam
   * @see McpJsonSchemaDefinition
   * @see McpJsonSchemaProperty
   * @see JavaTypeToJsonSchemaMapper
   */
  private McpSchema.JsonSchema createJsonSchema(
      McpApplicationContext context, Parameter[] methodParams) {
    Map<String, Object> properties = new LinkedHashMap<>();
    Map<String, Object> definitions = new LinkedHashMap<>();
    List<String> required = new ArrayList<>();

    for (Parameter param : methodParams) {
      if (param.isAnnotationPresent(McpToolParam.class)) {
        McpToolParam toolParam = param.getAnnotation(McpToolParam.class);
        final String parameterName = toolParam.name();
        Class<?> definitionClass = param.getType();
        Map<String, String> property = new HashMap<>();

        if (definitionClass.isAnnotationPresent(McpJsonSchemaDefinition.class)) {
          final String definitionClassName = definitionClass.getSimpleName();
          property.put("$ref", "#/definitions/" + definitionClassName);
          Map<String, Object> definition = createJsonSchemaDefinition(context, definitionClass);
          definitions.put(definitionClassName, definition);
        } else {
          property.put("type", JavaTypeToJsonSchemaMapper.getJsonSchemaType(definitionClass));
          property.put(
              "description", context.getLocalizedString(toolParam.description(), parameterName));
        }
        properties.put(parameterName, property);

        if (toolParam.required()) {
          required.add(parameterName);
        }
      }
    }

    final boolean hasAdditionalProperties = false;
    return new McpSchema.JsonSchema(
        JavaTypeToJsonSchemaMapper.OBJECT.getJsonSchemaType(),
        properties,
        required,
        hasAdditionalProperties,
        definitions,
        definitions);
  }

  /**
   * Creates a JSON schema definition for the specified class.
   *
   * <p>This private method processes a class annotated with {@link McpJsonSchemaDefinition} and
   * creates a JSON schema definition that describes the class structure. It examines all fields
   * annotated with {@link McpJsonSchemaProperty} and includes them in the schema with appropriate
   * types and descriptions.
   *
   * @param context the application context for localization
   * @param definitionClass the class to create a JSON schema definition for
   * @return a JSON schema definition describing the class structure
   * @see McpJsonSchemaDefinition
   * @see McpJsonSchemaProperty
   * @see JavaTypeToJsonSchemaMapper
   */
  private Map<String, Object> createJsonSchemaDefinition(
      McpApplicationContext context, Class<?> definitionClass) {
    Map<String, Object> definitionJsonSchema = new HashMap<>();
    definitionJsonSchema.put("type", JavaTypeToJsonSchemaMapper.OBJECT.getJsonSchemaType());

    Map<String, Object> properties = new LinkedHashMap<>();
    List<String> required = new ArrayList<>();

    Set<Field> definitionFields = context.getFieldsAnnotatedWith(McpJsonSchemaProperty.class);
    List<Field> fields =
        definitionFields.stream().filter(f -> f.getDeclaringClass() == definitionClass).toList();

    for (Field field : fields) {
      McpJsonSchemaProperty property = field.getAnnotation(McpJsonSchemaProperty.class);
      if (property == null) {
        continue;
      }

      Map<String, Object> fieldProperties = new HashMap<>();
      final String fieldName = StringHelper.defaultIfBlank(property.name(), field.getName());
      fieldProperties.put("type", JavaTypeToJsonSchemaMapper.getJsonSchemaType(field.getType()));
      fieldProperties.put(
          "description", context.getLocalizedString(property.description(), fieldName));

      properties.put(fieldName, fieldProperties);

      if (property.required()) {
        required.add(fieldName);
      }
    }

    definitionJsonSchema.put("properties", properties);
    definitionJsonSchema.put("required", required);

    return definitionJsonSchema;
  }
}
