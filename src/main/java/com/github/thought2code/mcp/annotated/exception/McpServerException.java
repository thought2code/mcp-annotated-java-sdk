package com.github.thought2code.mcp.annotated.exception;

/**
 * Base unchecked exception for MCP server runtime failures.
 *
 * <p>More specific failures use subclasses such as {@link McpServerConfigurationException} and
 * {@link McpServerComponentRegistrationException}.
 *
 * @author codeboyzhou
 */
public class McpServerException extends RuntimeException {

  /**
   * Creates a new instance of {@code McpServerException} with the specified detail message.
   *
   * @param message the detail message
   */
  public McpServerException(String message) {
    super(message);
  }

  /**
   * Creates a new instance of {@code McpServerException} with the specified detail message and
   * cause.
   *
   * @param message the detail message
   * @param cause the cause of the exception
   */
  public McpServerException(String message, Throwable cause) {
    super(message, cause);
  }
}
