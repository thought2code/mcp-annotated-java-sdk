package com.github.thought2code.mcp.annotated.server.converter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.util.Map;

/**
 * Converts MCP request argument maps into typed method parameters for annotated handlers.
 *
 * @param <A> parameter annotation type ({@link
 *     com.github.thought2code.mcp.annotated.annotation.McpToolParam} or {@link
 *     com.github.thought2code.mcp.annotated.annotation.McpPromptParam})
 * @author codeboyzhou
 */
public interface ParameterConverter<A extends Annotation> {
  /**
   * Converts the value of the specified parameter annotated with the specified annotation to the
   * required type.
   *
   * @param parameter the parameter to convert
   * @param annotation the annotation that annotates the parameter
   * @param args the arguments passed to the method
   * @return the converted value of the parameter
   */
  Object convert(Parameter parameter, A annotation, Map<String, Object> args);

  /**
   * Returns the type of the annotation that this converter supports.
   *
   * @return the type of the annotation that this converter supports
   */
  Class<A> getAnnotationType();
}
