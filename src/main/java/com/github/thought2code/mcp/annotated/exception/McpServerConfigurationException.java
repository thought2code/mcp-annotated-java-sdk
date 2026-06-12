package com.github.thought2code.mcp.annotated.exception;

import com.github.thought2code.mcp.annotated.configuration.ConfigurationLoader;

/**
 * Unchecked exception for invalid or unloadable MCP server configuration.
 *
 * <p>Thrown by {@link ConfigurationLoader} and configuration validation helpers when YAML cannot be
 * read or required settings are missing.
 *
 * @author codeboyzhou
 */
public class McpServerConfigurationException extends McpServerException {
  /**
   * Creates a new instance of {@code McpServerConfigurationException} with the specified detail
   * message.
   *
   * @param message the detail message
   */
  public McpServerConfigurationException(String message) {
    super(message);
  }

  /**
   * Creates a new instance of {@code McpServerConfigurationException} with the specified detail
   * message and cause.
   *
   * @param message the detail message
   * @param cause the cause of the exception
   */
  public McpServerConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }
}
