package com.github.thought2code.mcp.annotated.exception;

/**
 * Exception thrown when an error occurs during MCP server component registration.
 *
 * <p>This exception is used to indicate failures that occur while attempting to register MCP server
 * components such as resources, prompts, tools, or completion handlers. Common scenarios where this
 * exception may be thrown include:
 *
 * <ul>
 *   <li>Invalid method signatures for component handlers
 *   <li>Missing or incorrect annotations on component methods
 *   <li>Dependency injection failures during component creation
 *   <li>Configuration errors in component metadata
 *   <li>Build-time model loading or runtime registration failures
 * </ul>
 *
 * <p>This exception extends {@link McpServerException} and is part of the MCP annotated framework's
 * exception hierarchy, providing specific error information for component registration failures
 * while maintaining compatibility with the general MCP server exception handling.
 *
 * @author codeboyzhou
 */
public class McpServerComponentRegistrationException extends McpServerException {
  /**
   * Constructs a new {@code McpServerComponentRegistrationException} with the specified detail
   * message.
   *
   * <p>This constructor is typically used when the component registration failure can be described
   * by a single error message without an underlying cause. The message should provide clear
   * information about what went wrong during the registration process.
   *
   * @param message the detail message explaining the reason for the exception, may be null but
   *     should provide meaningful error information
   */
  public McpServerComponentRegistrationException(String message) {
    super(message);
  }

  /**
   * Constructs a new {@code McpServerComponentRegistrationException} with the specified detail
   * message and cause.
   *
   * <p>This constructor is typically used when the component registration failure is caused by
   * another exception. The cause exception is preserved and can be accessed later for detailed
   * error analysis and debugging.
   *
   * <p>Common causes include component instantiation failures, build-time model loading issues, or
   * validation errors during component registration. The message should provide context about the
   * registration operation that failed.
   *
   * @param message the detail message explaining the reason for the exception, may be null but
   *     should provide meaningful error information
   * @param cause the underlying cause of the exception, may be null if the cause is unknown or not
   *     applicable
   */
  public McpServerComponentRegistrationException(String message, Throwable cause) {
    super(message, cause);
  }
}
