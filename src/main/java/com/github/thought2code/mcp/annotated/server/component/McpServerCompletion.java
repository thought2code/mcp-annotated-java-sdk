package com.github.thought2code.mcp.annotated.server.component;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.annotation.McpPromptCompletion;
import com.github.thought2code.mcp.annotated.annotation.McpResourceCompletion;
import com.github.thought2code.mcp.annotated.exception.McpServerComponentRegistrationException;
import com.github.thought2code.mcp.annotated.reflect.Invocation;
import com.github.thought2code.mcp.annotated.reflect.MethodInvoker;
import com.github.thought2code.mcp.annotated.reflect.MethodMetadata;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/**
 * MCP server component for handling completion requests.
 *
 * <p>This class is responsible for creating and managing completion specifications for the Model
 * Context Protocol (MCP) server. It processes methods annotated with {@link McpPromptCompletion} or
 * {@link McpResourceCompletion} and creates the appropriate completion handlers that can provide
 * auto-completion suggestions.
 *
 * <p>The component validates method signatures, creates completion references, and handles the
 * invocation of completion methods. It supports both prompt-based and resource-based completion
 * functionality.
 *
 * @author codeboyzhou
 */
public class McpServerCompletion {

  private static final Logger log = LoggerFactory.getLogger(McpServerCompletion.class);

  /**
   * Retrieves all synchronous completion specifications from methods annotated with completion
   * annotations.
   *
   * <p>This static method scans for all methods annotated with either {@link McpPromptCompletion}
   * or {@link McpResourceCompletion} annotations and creates synchronous completion specifications
   * for each.
   *
   * @param context the application context for component discovery and localization
   * @return a list of synchronous completion specifications for all discovered completion methods
   * @see McpPromptCompletion
   * @see McpResourceCompletion
   * @see McpServerFeatures.SyncCompletionSpecification
   */
  public static List<McpServerFeatures.SyncCompletionSpecification> allSync(
      McpApplicationContext context) {
    Set<Method> methods = new HashSet<>();
    methods.addAll(context.getMethodsAnnotatedWith(McpPromptCompletion.class));
    methods.addAll(context.getMethodsAnnotatedWith(McpResourceCompletion.class));
    List<McpServerFeatures.SyncCompletionSpecification> completions = new ArrayList<>();
    methods.forEach(method -> completions.add(fromSync(method, context)));
    return completions;
  }

  /**
   * Retrieves all asynchronous completion specifications from methods annotated with completion
   * annotations.
   *
   * <p>This static method scans for all methods annotated with either {@link McpPromptCompletion}
   * or {@link McpResourceCompletion} annotations and creates asynchronous completion specifications
   * for each.
   *
   * @param context the application context for component discovery and localization
   * @return a list of asynchronous completion specifications for all discovered completion methods
   * @see McpPromptCompletion
   * @see McpResourceCompletion
   * @see McpServerFeatures.AsyncCompletionSpecification
   */
  public static List<McpServerFeatures.AsyncCompletionSpecification> allAsync(
      McpApplicationContext context) {
    Set<Method> methods = new HashSet<>();
    methods.addAll(context.getMethodsAnnotatedWith(McpPromptCompletion.class));
    methods.addAll(context.getMethodsAnnotatedWith(McpResourceCompletion.class));
    List<McpServerFeatures.AsyncCompletionSpecification> completions = new ArrayList<>();
    methods.forEach(method -> completions.add(fromAsync(method, context)));
    return completions;
  }

  /**
   * Creates a synchronous completion specification for the given method.
   *
   * <p>This method validates the method signature to ensure it meets the requirements for
   * completion handlers, then creates a {@link McpServerFeatures.SyncCompletionSpecification} that
   * can be registered with the MCP server.
   *
   * @param method the method to create completion specification for
   * @param context the application context for component discovery and localization
   * @return a synchronous completion specification for the MCP server
   * @throws McpServerComponentRegistrationException if the method signature is invalid
   */
  private static McpServerFeatures.SyncCompletionSpecification fromSync(
      Method method, McpApplicationContext context) {
    log.info("Creating sync completion specification for method: {}", method.toGenericString());

    Class<?> returnType = method.getReturnType();
    if (returnType != McpCompleteCompletion.class) {
      throw new McpServerComponentRegistrationException(
          "Completion method must return McpCompleteCompletion");
    }

    Parameter[] parameters = method.getParameters();
    if (parameters.length != 1
        || parameters[0].getType() != McpSchema.CompleteRequest.CompleteArgument.class) {
      throw new McpServerComponentRegistrationException(
          "Completion method must have exactly one parameter of type McpSchema.CompleteRequest.CompleteArgument");
    }

    Object instance = context.getComponentInstance(method.getDeclaringClass());
    McpSchema.CompleteReference reference = createCompleteReference(method);
    return new McpServerFeatures.SyncCompletionSpecification(
        reference, (exchange, request) -> invoke(instance, method, request));
  }

  /**
   * Creates an asynchronous completion specification for the given method.
   *
   * <p>This method validates the method signature to ensure it meets the requirements for
   * completion handlers, then creates a {@link McpServerFeatures.AsyncCompletionSpecification} that
   * can be registered with the MCP async server. The handler wraps the synchronous invocation
   * result in a {@link Mono}.
   *
   * @param method the method to create completion specification for
   * @param context the application context for component discovery and localization
   * @return an asynchronous completion specification for the MCP server
   * @throws McpServerComponentRegistrationException if the method signature is invalid
   */
  private static McpServerFeatures.AsyncCompletionSpecification fromAsync(
      Method method, McpApplicationContext context) {
    log.info("Creating async completion specification for method: {}", method.toGenericString());

    Class<?> returnType = method.getReturnType();
    if (returnType != McpCompleteCompletion.class) {
      throw new McpServerComponentRegistrationException(
          "Completion method must return McpCompleteCompletion");
    }

    Parameter[] parameters = method.getParameters();
    if (parameters.length != 1
        || parameters[0].getType() != McpSchema.CompleteRequest.CompleteArgument.class) {
      throw new McpServerComponentRegistrationException(
          "Completion method must have exactly one parameter of type McpSchema.CompleteRequest.CompleteArgument");
    }

    Object instance = context.getComponentInstance(method.getDeclaringClass());
    McpSchema.CompleteReference reference = createCompleteReference(method);
    return new McpServerFeatures.AsyncCompletionSpecification(
        reference,
        (exchange, request) -> Mono.fromCallable(() -> invoke(instance, method, request)));
  }

  /**
   * Invokes the completion method with the provided request.
   *
   * <p>This private method handles the actual invocation of the completion method, extracting the
   * completion argument from the request and converting the result into the appropriate MCP schema
   * format.
   *
   * <p>The method uses reflection to invoke the target method and converts the {@link
   * McpCompleteCompletion} result into a {@link McpSchema.CompleteResult} that can be returned to
   * the MCP client.
   *
   * @param instance the object instance containing the completion method
   * @param method the completion method to invoke
   * @param request the completion request containing the argument
   * @return the completion result in MCP schema format
   * @throws RuntimeException if the method invocation fails
   * @see McpCompleteCompletion
   * @see McpSchema.CompleteResult
   * @see Invocation
   */
  private static McpSchema.CompleteResult invoke(
      Object instance, Method method, McpSchema.CompleteRequest request) {

    MethodMetadata metadata = MethodMetadata.of(method);
    McpSchema.CompleteRequest.CompleteArgument argument = request.argument();
    Invocation invocation = MethodInvoker.invoke(instance, method, metadata, argument);
    McpCompleteCompletion completion = (McpCompleteCompletion) invocation.result();
    return new McpSchema.CompleteResult(
        new McpSchema.CompleteResult.CompleteCompletion(
            completion.values(), completion.total(), completion.hasMore()));
  }

  /**
   * Creates a completion reference based on the method's annotations.
   *
   * <p>This private method examines the method's annotations to determine whether it's a
   * prompt-based or resource-based completion and creates the appropriate reference object
   * accordingly.
   *
   * <p>The method checks for {@link McpPromptCompletion} annotation first, then {@link
   * McpResourceCompletion}. If neither annotation is present, it returns null, though this should
   * never happen in normal operation due to prior validation.
   *
   * @param method the method object to examine annotations
   * @return a completion reference (either {@link McpSchema.PromptReference} or {@link
   *     McpSchema.ResourceReference}), or null if no valid annotation is found
   * @see McpPromptCompletion
   * @see McpResourceCompletion
   * @see McpSchema.PromptReference
   * @see McpSchema.ResourceReference
   */
  private static McpSchema.CompleteReference createCompleteReference(Method method) {
    McpPromptCompletion prompt = method.getAnnotation(McpPromptCompletion.class);
    if (prompt != null) {
      final String name = prompt.name();
      final String title = prompt.title();
      return McpSchema.PromptReference.builder(name).title(title).build();
    }

    McpResourceCompletion resource = method.getAnnotation(McpResourceCompletion.class);
    if (resource != null) {
      final String uri = resource.uri();
      return new McpSchema.ResourceReference(uri);
    }

    // should never happen
    return null;
  }
}
