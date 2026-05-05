package com.github.thought2code.mcp.annotated.server.component;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.annotation.McpPrompt;
import com.github.thought2code.mcp.annotated.annotation.McpPromptParam;
import com.github.thought2code.mcp.annotated.reflect.Invocation;
import com.github.thought2code.mcp.annotated.reflect.MethodInvoker;
import com.github.thought2code.mcp.annotated.reflect.MethodMetadata;
import com.github.thought2code.mcp.annotated.server.converter.McpPromptParameterConverter;
import com.github.thought2code.mcp.annotated.util.JacksonHelper;
import com.github.thought2code.mcp.annotated.util.StringHelper;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/**
 * MCP server component for handling prompt-related operations.
 *
 * <p>This class implements the functionality for creating and registering prompt components with an
 * MCP server. It processes methods annotated with {@link McpPrompt} and creates appropriate prompt
 * specifications that can be used to generate interactive prompts for LLM interactions.
 *
 * <p>The class handles:
 *
 * <ul>
 *   <li>Creation of prompt specifications from annotated methods
 *   <li>Registration of all prompt components with the server
 *   <li>Invocation of prompt methods with proper argument conversion
 *   <li>Localization of prompt attributes using resource bundles
 * </ul>
 *
 * @author codeboyzhou
 * @see McpPrompt
 * @see McpPromptParam
 * @see McpSchema.Prompt
 * @see McpSchema.PromptArgument
 */
public class McpServerPrompt
    implements McpServerComponent<McpServerFeatures.SyncPromptSpecification>,
        McpComponentRegistrar {

  private static final Logger log = LoggerFactory.getLogger(McpServerPrompt.class);

  /** The converter for MCP prompt parameters. */
  private final McpPromptParameterConverter parameterConverter;

  /** Constructor that initializes the prompt parameter converter. */
  public McpServerPrompt() {
    this.parameterConverter = new McpPromptParameterConverter();
  }

  /**
   * Creates a synchronous prompt specification from the specified method.
   *
   * <p>This method processes a method annotated with {@link McpPrompt} and creates a {@link
   * McpServerFeatures.SyncPromptSpecification} that can be registered with the MCP server. The
   * method extracts prompt information from annotations and method signature, and creates
   * appropriate prompt arguments.
   *
   * @param method the method annotated with {@link McpPrompt} to create a specification from
   * @param context the application context for component discovery and localization
   * @return a synchronous prompt specification for the MCP server
   * @see McpPrompt
   * @see McpSchema.Prompt
   * @see McpSchema.PromptArgument
   */
  @Override
  public McpServerFeatures.SyncPromptSpecification from(
      Method method, McpApplicationContext context) {
    log.info("Creating sync prompt specification for method: {}", method.toGenericString());

    McpPrompt mcpPrompt = method.getAnnotation(McpPrompt.class);
    final String name = StringHelper.defaultIfBlank(mcpPrompt.name(), method.getName());
    final String title = context.getLocalizedString(mcpPrompt.title(), name);
    final String description = context.getLocalizedString(mcpPrompt.description(), name);

    List<McpSchema.PromptArgument> args = createPromptArguments(context, method.getParameters());
    McpSchema.Prompt prompt = new McpSchema.Prompt(name, title, description, args);

    log.info("Sync prompt specification created: {}", JacksonHelper.toJsonString(prompt));

    Object instance = context.getComponentInstance(method.getDeclaringClass());

    return new McpServerFeatures.SyncPromptSpecification(
        prompt, (exchange, request) -> invoke(instance, method, description, request));
  }

  /**
   * Creates an asynchronous prompt specification from the specified method.
   *
   * <p>This method processes a method annotated with {@link McpPrompt} and creates a {@link
   * McpServerFeatures.AsyncPromptSpecification} that can be registered with the MCP async server.
   * The handler wraps the synchronous invocation result in a {@link Mono}.
   *
   * @param method the method annotated with {@link McpPrompt} to create a specification from
   * @param context the application context for component discovery and localization
   * @return an asynchronous prompt specification for the MCP server
   */
  public McpServerFeatures.AsyncPromptSpecification fromAsync(
      Method method, McpApplicationContext context) {
    log.info("Creating async prompt specification for method: {}", method.toGenericString());

    McpPrompt mcpPrompt = method.getAnnotation(McpPrompt.class);
    final String name = StringHelper.defaultIfBlank(mcpPrompt.name(), method.getName());
    final String title = context.getLocalizedString(mcpPrompt.title(), name);
    final String description = context.getLocalizedString(mcpPrompt.description(), name);

    List<McpSchema.PromptArgument> args = createPromptArguments(context, method.getParameters());
    McpSchema.Prompt prompt = new McpSchema.Prompt(name, title, description, args);

    log.info("Async prompt specification created: {}", JacksonHelper.toJsonString(prompt));

    Object instance = context.getComponentInstance(method.getDeclaringClass());

    return new McpServerFeatures.AsyncPromptSpecification(
        prompt,
        (exchange, request) ->
            Mono.fromCallable(() -> invoke(instance, method, description, request)));
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
    Set<Method> methods = context.getMethodsAnnotatedWith(McpPrompt.class);
    for (Method method : methods) {
      log.debug("Registering prompt method: {}", method.toGenericString());
      McpServerFeatures.SyncPromptSpecification prompt = from(method, context);
      server.addPrompt(prompt);
      log.debug("Prompt {} registered successfully", prompt.prompt().name());
    }
  }

  @Override
  public void register(McpAsyncServer server, McpApplicationContext context) {
    Set<Method> methods = context.getMethodsAnnotatedWith(McpPrompt.class);
    for (Method method : methods) {
      log.debug("Registering async prompt method: {}", method.toGenericString());
      McpServerFeatures.AsyncPromptSpecification prompt = fromAsync(method, context);
      server.addPrompt(prompt).subscribe();
      log.debug("Async prompt {} registered successfully", prompt.prompt().name());
    }
  }

  /**
   * Invokes the prompt method with the specified arguments and request.
   *
   * <p>This private method handles the actual invocation of the prompt method, converting request
   * arguments to the appropriate parameter types and invoking the method using reflection. The
   * result is then wrapped in a {@link McpSchema.GetPromptResult} with the prompt description.
   *
   * @param instance the object instance containing the prompt method
   * @param method the prompt method to invoke
   * @param description the description of the prompt
   * @param request the prompt request containing the arguments
   * @return the result of the prompt invocation
   * @see McpSchema.GetPromptResult
   * @see McpSchema.PromptMessage
   * @see McpSchema.Content
   */
  private McpSchema.GetPromptResult invoke(
      Object instance, Method method, String description, McpSchema.GetPromptRequest request) {

    log.debug("Handling MCP GetPromptRequest: {}", JacksonHelper.toJsonString(request));

    MethodMetadata metadata = MethodMetadata.of(method);
    Parameter[] parameters = metadata.getParameters();
    Map<String, Object> arguments = request.arguments();
    List<Object> params = parameterConverter.convertAll(parameters, arguments);
    Invocation invocation = MethodInvoker.invoke(instance, method, metadata, params);

    McpSchema.Content content = new McpSchema.TextContent(invocation.result().toString());
    McpSchema.PromptMessage message = new McpSchema.PromptMessage(McpSchema.Role.USER, content);
    McpSchema.GetPromptResult getPromptResult =
        new McpSchema.GetPromptResult(description, List.of(message));

    log.debug("Returning MCP GetPromptResult: {}", JacksonHelper.toJsonString(getPromptResult));

    return getPromptResult;
  }

  /**
   * Creates a list of prompt arguments from the method parameters.
   *
   * <p>This private method processes method parameters and creates a list of {@link
   * McpSchema.PromptArgument} objects for parameters annotated with {@link McpPromptParam}. Each
   * argument includes name, title, description, and required status.
   *
   * @param methodParams the array of method parameters to process
   * @return a list of prompt arguments for the method
   * @see McpPromptParam
   * @see McpSchema.PromptArgument
   */
  private List<McpSchema.PromptArgument> createPromptArguments(
      McpApplicationContext context, Parameter[] methodParams) {
    List<McpSchema.PromptArgument> promptArguments = new ArrayList<>(methodParams.length);

    for (Parameter param : methodParams) {
      if (param.isAnnotationPresent(McpPromptParam.class)) {
        McpPromptParam promptParam = param.getAnnotation(McpPromptParam.class);
        final String name = promptParam.name();
        final String title = context.getLocalizedString(promptParam.title(), name);
        final String description = context.getLocalizedString(promptParam.description(), name);
        final boolean required = promptParam.required();
        promptArguments.add(new McpSchema.PromptArgument(name, title, description, required));
      }
    }

    return promptArguments;
  }
}
