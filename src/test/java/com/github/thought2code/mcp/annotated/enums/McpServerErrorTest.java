package com.github.thought2code.mcp.annotated.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class McpServerErrorTest {

  @Test
  void toString_shouldFormatCodeAndMessage() {
    assertEquals(
        "[MCP_METHOD_INVOCATION_ERROR] Internal server error while executing MCP method.",
        McpServerError.METHOD_INVOCATION_ERROR.toString());
  }

  @Test
  void withDetail_shouldAppendDetailToFormattedMessage() {
    String message = McpServerError.CONFIG_FILE_NOT_FOUND.withDetail("mcp-server.yml");
    assertTrue(message.contains(McpServerError.CONFIG_FILE_NOT_FOUND.getCode()));
    assertTrue(message.contains("mcp-server.yml"));
  }

  @Test
  void getters_shouldExposeCodeAndMessage() {
    assertEquals("MCP_JSON_SERIALIZE_ERROR", McpServerError.JSON_SERIALIZE_ERROR.getCode());
    assertEquals(
        "Error serializing object to JSON.", McpServerError.JSON_SERIALIZE_ERROR.getMessage());
  }
}
