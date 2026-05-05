package com.github.thought2code.mcp.annotated.enums;

public enum McpServerError {
  CONFIG_FILE_NOT_FOUND("MCP_CONFIG_FILE_NOT_FOUND", "Configuration file not found."),
  INVALID_CONFIG_FILE("MCP_INVALID_CONFIG_FILE", "Invalid configuration file."),
  YAML_READ_ERROR("MCP_YAML_READ_ERROR", "Error reading YAML configuration file."),
  JETTY_SERVER_START_ERROR(
      "MCP_JETTY_SERVER_START_ERROR", "Failed to start Jetty-based MCP server."),
  COMPONENT_INSTANCE_CREATE_ERROR(
      "MCP_COMPONENT_INSTANCE_CREATE_ERROR", "Failed to create component instance."),
  METHOD_INVOCATION_ERROR(
      "MCP_METHOD_INVOCATION_ERROR", "Internal server error while executing MCP method.");

  private final String code;
  private final String message;

  McpServerError(String code, String message) {
    this.code = code;
    this.message = message;
  }

  public String getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }

  public String withDetail(String detail) {
    return String.format("[%s] %s Detail: %s", getCode(), getMessage(), detail);
  }

  @Override
  public String toString() {
    return String.format("[%s] %s", getCode(), getMessage());
  }
}
