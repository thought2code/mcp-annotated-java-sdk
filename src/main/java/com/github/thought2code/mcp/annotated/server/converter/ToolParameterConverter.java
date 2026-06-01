package com.github.thought2code.mcp.annotated.server.converter;

import com.github.thought2code.mcp.annotated.annotation.McpToolParam;
import com.github.thought2code.mcp.annotated.util.TypeConverter;
import java.lang.reflect.Parameter;
import java.util.Map;

/**
 * This class is used to convert the value of a parameter annotated with {@link McpToolParam} to the
 * required type.
 *
 * @author codeboyzhou
 */
public class ToolParameterConverter extends AbstractParameterConverter<McpToolParam> {
  /**
   * Converts the value of the parameter annotated with {@link McpToolParam} to the required type.
   *
   * @param parameter the parameter annotated with {@link McpToolParam}
   * @param annotation the annotation instance
   * @param args the arguments passed to the method
   * @return the converted value of the parameter
   */
  @Override
  public Object convert(Parameter parameter, McpToolParam annotation, Map<String, Object> args) {
    Object rawValue = args.get(annotation.name());
    return TypeConverter.convert(rawValue, parameter.getType());
  }

  /**
   * Returns the type of the annotation this converter handles.
   *
   * @return the type of the annotation this converter handles
   */
  @Override
  public Class<McpToolParam> getAnnotationType() {
    return McpToolParam.class;
  }
}
