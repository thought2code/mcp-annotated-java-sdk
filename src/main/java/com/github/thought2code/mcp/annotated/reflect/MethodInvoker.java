package com.github.thought2code.mcp.annotated.reflect;

import com.github.thought2code.mcp.annotated.enums.McpServerError;
import com.github.thought2code.mcp.annotated.exception.McpServerException;
import io.modelcontextprotocol.spec.McpSchema;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class for performing reflection operations including instance creation and method
 * invocation.
 *
 * <p>This class provides static methods for:
 *
 * <ul>
 *   <li>Creating instances of classes using reflection
 *   <li>Invoking methods with various parameter configurations
 *   <li>Handling method invocation results and exceptions
 * </ul>
 *
 * <p>All method invocations are wrapped with proper error handling, and results are encapsulated in
 * {@link Invocation} objects for consistent error reporting. The class follows the utility class
 * pattern with a private constructor to prevent instantiation.
 *
 * @author codeboyzhou
 * @see Invocation
 * @see Method
 */
public final class MethodInvoker {

  private static final Logger log = LoggerFactory.getLogger(MethodInvoker.class);

  /** Private constructor to prevent instantiation of this utility class. */
  private MethodInvoker() {}

  /**
   * Creates a new instance of the specified class using reflection.
   *
   * <p>This static method uses reflection to create a new instance of the given class by calling
   * its no-argument constructor. The method requires that the class has a public no-argument
   * constructor.
   *
   * @param clazz the class to instantiate
   * @return a new instance of the specified class
   * @throws McpServerException if the instance creation fails due to any reason
   * @see Class#getDeclaredConstructor(Class[])
   * @see Constructor#newInstance(Object...)
   */
  public static Object createInstance(Class<?> clazz) {
    try {
      return clazz.getDeclaredConstructor().newInstance();
    } catch (Exception e) {
      throw new McpServerException(
          McpServerError.COMPONENT_INSTANCE_CREATE_ERROR.withDetail(clazz.getName()), e);
    }
  }

  /**
   * Invokes the method represented by the specified method cache on the given instance with the
   * provided parameters.
   *
   * <p>This method uses reflection to invoke the specified method on the target instance, passing
   * the provided parameters. The method handles various return types and null values:
   *
   * <ul>
   *   <li>Void return types: Returns a success message indicating the method completed
   *   <li>Null return values: Returns a message indicating the method succeeded but returned null
   *   <li>Non-null return values: Returns the actual result
   * </ul>
   *
   * <p>All exceptions are caught and wrapped in an {@link Invocation} with appropriate error
   * messages. The method signature is logged for debugging purposes when an error occurs.
   *
   * @param instance the instance on which to invoke the method
   * @param method the reflected method to invoke
   * @param metadata the method metadata (cached)
   * @param params the list of parameters to pass to the method
   * @return an InvocationResult containing the method result or error information
   * @see MethodMetadata
   * @see Invocation
   * @see Method#invoke(Object, Object...)
   */
  public static Invocation invoke(
      Object instance, Method method, MethodMetadata metadata, List<Object> params) {
    Invocation.Builder builder = Invocation.builder();
    try {
      Object result = method.invoke(instance, params.toArray());

      Class<?> returnType = metadata.getReturnType();
      if (returnType == void.class || returnType == Void.class) {
        return builder.result("The method call succeeded but has a void return type").build();
      }

      final String resultIfNull = "The method call succeeded but the return value is null";
      return builder.result(Objects.requireNonNullElse(result, resultIfNull)).build();
    } catch (Exception e) {
      final String message = "Error invoking method: " + method.toGenericString();
      final String result = McpServerError.METHOD_INVOCATION_ERROR.toString();
      Invocation invocation = builder.result(result).isError(true).build();
      log.error(message, e);
      return invocation;
    }
  }

  /**
   * Invokes the method represented by the specified method cache on the given instance with no
   * parameters.
   *
   * <p>This is a convenience method that invokes a method with an empty parameter list. It
   * delegates to {@link #invoke(Object, Method, MethodMetadata, List)} with an empty list.
   *
   * @param instance the instance on which to invoke the method
   * @param method the reflected method to invoke
   * @param metadata the method metadata (cached)
   * @return an InvocationResult containing the method result or error information
   * @see #invoke(Object, Method, MethodMetadata, List)
   */
  public static Invocation invoke(Object instance, Method method, MethodMetadata metadata) {
    return invoke(instance, method, metadata, List.of());
  }

  /**
   * Invokes the method represented by the specified method cache on the given instance with the
   * provided completion argument.
   *
   * <p>This is a convenience method for invoking methods that take a single {@link
   * McpSchema.CompleteRequest.CompleteArgument} parameter. It wraps the argument in a list and
   * delegates to {@link #invoke(Object, Method, MethodMetadata, List)}.
   *
   * <p>This method is typically used for MCP completion operations where a single completion
   * argument needs to be passed to the method.
   *
   * @param instance the instance on which to invoke the method
   * @param method the reflected method to invoke
   * @param metadata the method metadata (cached)
   * @param argument the completion argument to pass to the method
   * @return an InvocationResult containing the method result or error information
   * @see #invoke(Object, Method, MethodMetadata, List)
   * @see McpSchema.CompleteRequest.CompleteArgument
   */
  public static Invocation invoke(
      Object instance,
      Method method,
      MethodMetadata metadata,
      McpSchema.CompleteRequest.CompleteArgument argument) {
    return invoke(instance, method, metadata, List.of(argument));
  }
}
