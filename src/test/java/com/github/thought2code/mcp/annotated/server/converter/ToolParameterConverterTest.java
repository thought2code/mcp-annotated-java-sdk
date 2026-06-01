package com.github.thought2code.mcp.annotated.server.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.thought2code.mcp.annotated.annotation.McpToolParam;
import com.github.thought2code.mcp.annotated.util.StringHelper;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ToolParameterConverterTest {

  private final ToolParameterConverter converter = new ToolParameterConverter();

  @Test
  void convertAll_shouldConvertAnnotatedParametersAndDefaultUnannotatedOnes() throws Exception {
    Method method = ToolMethods.class.getDeclaredMethod("mixedParams", String.class, String.class);
    List<Object> converted =
        converter.convertAll(method.getParameters(), Map.of("mcpParam", "value"));

    assertEquals("value", converted.get(0));
    assertEquals(StringHelper.EMPTY, converted.get(1));
  }

  @Test
  void convertAll_shouldConvertNumericParameterTypes() throws Exception {
    Method method = ToolMethods.class.getDeclaredMethod("numericParam", int.class);
    List<Object> converted = converter.convertAll(method.getParameters(), Map.of("param", "42"));
    assertEquals(42, converted.get(0));
  }

  static class ToolMethods {
    // This test verifies parameter conversion only; @McpTool is intentionally omitted to avoid
    // triggering annotation-processor component validation during test-compile.
    public String mixedParams(
        @McpToolParam(name = "mcpParam") String mcpParam, String nonMcpParam) {
      return mcpParam + nonMcpParam;
    }

    // This test verifies parameter conversion only; @McpTool is intentionally omitted to avoid
    // triggering annotation-processor component validation during test-compile.
    public String numericParam(@McpToolParam(name = "param") int param) {
      return Integer.toString(param);
    }
  }
}
