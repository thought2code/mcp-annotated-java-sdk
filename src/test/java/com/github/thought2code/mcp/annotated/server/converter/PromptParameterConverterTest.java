package com.github.thought2code.mcp.annotated.server.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.thought2code.mcp.annotated.annotation.McpPromptParam;
import com.github.thought2code.mcp.annotated.util.StringHelper;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PromptParameterConverterTest {

  private final PromptParameterConverter converter = new PromptParameterConverter();

  @Test
  void convertAll_shouldConvertAnnotatedParametersAndDefaultUnannotatedOnes() throws Exception {
    Method method =
        PromptMethods.class.getDeclaredMethod("mixedParams", String.class, String.class);
    List<Object> converted =
        converter.convertAll(method.getParameters(), Map.of("mcpParam", "value"));

    assertEquals("value", converted.get(0));
    assertEquals(StringHelper.EMPTY, converted.get(1));
  }

  static class PromptMethods {
    // This test verifies parameter conversion only; @McpPrompt is intentionally omitted to avoid
    // triggering annotation-processor component validation during test-compile.
    public String mixedParams(
        @McpPromptParam(name = "mcpParam") String mcpParam, String nonMcpParam) {
      return mcpParam + nonMcpParam;
    }
  }
}
