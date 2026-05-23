package com.github.thought2code.mcp.annotated.server.component;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.annotation.McpPrompt;
import com.github.thought2code.mcp.annotated.annotation.McpPromptParam;
import com.github.thought2code.mcp.annotated.enums.ServerType;
import com.github.thought2code.mcp.annotated.reflect.ComponentInvocationSupport;
import com.github.thought2code.mcp.annotated.reflect.Invocation;
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
    extends AbstractDualModeComponentRegistrar<
        McpServerFeatures.SyncPromptSpecification, McpServerFeatures.AsyncPromptSpecification>
    implements McpServerComponent<McpServerFeatures.SyncPromptSpecification> {

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
    PromptDefinition definition = createPromptDefinition(method, context, ServerType.SYNC);
    return new McpServerFeatures.SyncPromptSpecification(
        definition.prompt(),
        (exchange, request) ->
            invoke(definition.instance(), method, definition.description(), request));
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
    PromptDefinition definition = createPromptDefinition(method, context, ServerType.ASYNC);
    return new McpServerFeatures.AsyncPromptSpecification(
        definition.prompt(),
        (exchange, request) ->
            Mono.fromCallable(
                () -> invoke(definition.instance(), method, definition.description(), request)));
  }

  @Override
  protected Class<McpPrompt> annotationType() {
    return McpPrompt.class;
  }

  @Override
  protected McpServerFeatures.SyncPromptSpecification createSyncSpecification(
      Method method, McpApplicationContext context) {
    return from(method, context);
  }

  @Override
  protected McpServerFeatures.AsyncPromptSpecification createAsyncSpecification(
      Method method, McpApplicationContext context) {
    return fromAsync(method, context);
  }

  @Override
  protected void addSyncSpecification(
      McpSyncServer server, McpServerFeatures.SyncPromptSpecification specification) {
    server.addPrompt(specification);
  }

  @Override
  protected Mono<Void> addAsyncSpecification(
      McpAsyncServer server, McpServerFeatures.AsyncPromptSpecification specification) {
    return server.addPrompt(specification);
  }

  @Override
  protected String syncSpecificationName(McpServerFeatures.SyncPromptSpecification specification) {
    return specification.prompt().name();
  }

  @Override
  protected String asyncSpecificationName(
      McpServerFeatures.AsyncPromptSpecification specification) {
    return specification.prompt().name();
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

    Invocation invocation =
        ComponentInvocationSupport.invokeWithParameters(
            instance, method, parameterConverter, request.arguments());

    McpSchema.Content content = McpSchema.TextContent.builder(invocation.asText()).build();
    McpSchema.PromptMessage message =
        McpSchema.PromptMessage.builder(McpSchema.Role.USER, content).build();
    McpSchema.GetPromptResult getPromptResult =
        McpSchema.GetPromptResult.builder(List.of(message)).description(description).build();

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
        McpSchema.PromptArgument argument =
            McpSchema.PromptArgument.builder(name)
                .title(title)
                .description(description)
                .required(required)
                .build();
        promptArguments.add(argument);
      }
    }

    return promptArguments;
  }

  /**
   * Creates the shared prompt definition used by sync and async specifications.
   *
   * @param method annotated prompt method
   * @param context application context
   * @param serverType server type (sync or async)
   * @return prompt definition containing metadata and target instance
   */
  private PromptDefinition createPromptDefinition(
      Method method, McpApplicationContext context, ServerType serverType) {
    McpPrompt mcpPrompt = method.getAnnotation(McpPrompt.class);
    final String name = StringHelper.defaultIfBlank(mcpPrompt.name(), method.getName());
    final String title = context.getLocalizedString(mcpPrompt.title(), name);
    final String description = context.getLocalizedString(mcpPrompt.description(), name);
    List<McpSchema.PromptArgument> args = createPromptArguments(context, method.getParameters());
    McpSchema.Prompt prompt =
        McpSchema.Prompt.builder(name)
            .title(title)
            .description(description)
            .arguments(args)
            .build();
    log.info(
        "{} prompt specification created: {}",
        serverType.description(),
        JacksonHelper.toJsonString(prompt));
    Object instance = context.getComponentInstance(method.getDeclaringClass());
    return new PromptDefinition(prompt, instance, description);
  }

  /** Prompt definition containing metadata and target instance. */
  private record PromptDefinition(McpSchema.Prompt prompt, Object instance, String description) {}
}
