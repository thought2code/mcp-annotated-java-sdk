package com.github.thought2code.mcp.annotated.reflect;

import com.github.thought2code.mcp.annotated.enums.McpServerError;
import com.github.thought2code.mcp.annotated.exception.McpServerException;
import com.github.thought2code.mcp.annotated.server.converter.AbstractParameterConverter;
import io.modelcontextprotocol.spec.McpSchema;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared invocation flow for MCP server components (tool, prompt, resource, completion).
 *
 * <p>Centralizes parameter conversion and method invocation so all component types apply the same
 * sanitized error contract from {@link MethodInvoker}.
 *
 * @author codeboyzhou
 */
public final class ComponentInvocationSupport {

  private static final Logger log = LoggerFactory.getLogger(ComponentInvocationSupport.class);

  private ComponentInvocationSupport() {}

  /**
   * Converts request arguments and invokes the target method.
   *
   * @param instance component instance
   * @param method target method
   * @param parameterConverter converter for annotated parameters
   * @param arguments request arguments
   * @return invocation result, including sanitized errors from conversion or method failures
   */
  public static Invocation invokeWithParameters(
      Object instance,
      Method method,
      AbstractParameterConverter<?> parameterConverter,
      Map<String, Object> arguments) {
    MethodMetadata metadata = MethodMetadata.of(method);
    try {
      List<Object> params = parameterConverter.convertAll(metadata.getParameters(), arguments);
      return MethodInvoker.invoke(instance, method, metadata, params);
    } catch (RuntimeException e) {
      return parameterConversionFailure(e);
    }
  }

  /**
   * Invokes a no-argument component method.
   *
   * @param instance component instance
   * @param method target method
   * @return invocation result
   */
  public static Invocation invoke(Object instance, Method method) {
    return MethodInvoker.invoke(instance, method, MethodMetadata.of(method));
  }

  /**
   * Invokes a completion handler method.
   *
   * @param instance component instance
   * @param method target method
   * @param argument completion argument from the request
   * @return invocation result
   */
  public static Invocation invoke(
      Object instance, Method method, McpSchema.CompleteRequest.CompleteArgument argument) {
    MethodMetadata metadata = MethodMetadata.of(method);
    return MethodInvoker.invoke(instance, method, metadata, argument);
  }

  private static Invocation parameterConversionFailure(RuntimeException e) {
    log.error("Parameter conversion failed", e);
    return Invocation.builder()
        .result(McpServerError.METHOD_INVOCATION_ERROR.toString())
        .isError(true)
        .build();
  }

  /**
   * Throws {@link McpServerException} when the invocation failed.
   *
   * @param invocation invocation to check
   */
  public static void throwIfError(Invocation invocation) {
    if (invocation.isError()) {
      throw new McpServerException(invocation.result().toString());
    }
  }

  /**
   * Returns the invocation result when successful.
   *
   * @param invocation invocation to check
   * @param type expected result type
   * @param <T> result type
   * @return typed invocation result
   * @throws McpServerException when the invocation failed
   */
  @SuppressWarnings("unchecked")
  public static <T> T requireResult(Invocation invocation, Class<T> type) {
    throwIfError(invocation);
    return (T) invocation.result();
  }
}
