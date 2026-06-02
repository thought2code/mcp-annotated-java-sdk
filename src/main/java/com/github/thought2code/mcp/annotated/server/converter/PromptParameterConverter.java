package com.github.thought2code.mcp.annotated.server.converter;

import com.github.thought2code.mcp.annotated.annotation.McpPromptParam;
import com.github.thought2code.mcp.annotated.util.TypeConverter;
import java.lang.reflect.Parameter;
import java.util.Map;

/**
 * {@link ParameterConverter} for {@link McpPromptParam}-annotated prompt handler parameters.
 *
 * @author codeboyzhou
 */
public class PromptParameterConverter extends AbstractParameterConverter<McpPromptParam> {
  /**
   * Converts the value of the parameter annotated with {@link McpPromptParam} to the required type.
   *
   * @param parameter the parameter annotated with {@link McpPromptParam}
   * @param annotation the annotation instance
   * @param args the arguments passed to the method
   * @return the converted value of the parameter
   */
  @Override
  public Object convert(Parameter parameter, McpPromptParam annotation, Map<String, Object> args) {
    Object rawValue = args.get(annotation.name());
    return TypeConverter.convert(rawValue, parameter.getType());
  }

  /**
   * Returns the type of the annotation this converter handles.
   *
   * @return the type of the annotation this converter handles
   */
  @Override
  public Class<McpPromptParam> getAnnotationType() {
    return McpPromptParam.class;
  }
}
