package com.github.thought2code.mcp.annotated.enums;

/**
 * Stable error codes and messages for MCP server failures.
 *
 * @author codeboyzhou
 */
public enum McpServerError {

  /** Configuration file was not found on the classpath. */
  CONFIG_FILE_NOT_FOUND("MCP_CONFIG_FILE_NOT_FOUND", "Configuration file not found."),

  /** Configuration file path or URI is invalid. */
  INVALID_CONFIG_FILE("MCP_INVALID_CONFIG_FILE", "Invalid configuration file."),

  /** YAML configuration file could not be read or parsed. */
  YAML_READ_ERROR("MCP_YAML_READ_ERROR", "Error reading YAML configuration file."),

  /** Object could not be serialized to JSON. */
  JSON_SERIALIZE_ERROR("MCP_JSON_SERIALIZE_ERROR", "Error serializing object to JSON."),

  /** JSON payload could not be deserialized. */
  JSON_DESERIALIZE_ERROR("MCP_JSON_DESERIALIZE_ERROR", "Error deserializing JSON."),

  /** Jetty HTTP server failed to start. */
  JETTY_SERVER_START_ERROR(
      "MCP_JETTY_SERVER_START_ERROR", "Failed to start Jetty-based MCP server."),

  /** MCP component instance could not be created. */
  COMPONENT_INSTANCE_CREATE_ERROR(
      "MCP_COMPONENT_INSTANCE_CREATE_ERROR", "Failed to create component instance."),

  /** Annotated MCP method invocation failed unexpectedly. */
  METHOD_INVOCATION_ERROR(
      "MCP_METHOD_INVOCATION_ERROR", "Internal server error while executing MCP method.");

  private final String code;
  private final String message;

  McpServerError(String code, String message) {
    this.code = code;
    this.message = message;
  }

  /**
   * Returns the stable machine-readable error code.
   *
   * @return the error code
   */
  public String getCode() {
    return code;
  }

  /**
   * Returns the human-readable error message.
   *
   * @return the error message
   */
  public String getMessage() {
    return message;
  }

  /**
   * Formats this error with an additional detail suffix.
   *
   * @param detail contextual detail appended to the message
   * @return formatted error text including code, message, and detail
   */
  public String withDetail(String detail) {
    return String.format("[%s] %s Detail: %s", getCode(), getMessage(), detail);
  }

  @Override
  public String toString() {
    return String.format("[%s] %s", getCode(), getMessage());
  }
}
