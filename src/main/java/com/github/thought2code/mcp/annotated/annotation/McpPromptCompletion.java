package com.github.thought2code.mcp.annotated.annotation;

import com.github.thought2code.mcp.annotated.server.component.completion.McpCompleteCompletion;
import com.github.thought2code.mcp.annotated.util.StringHelper;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for marking methods that provide completion functionality for MCP prompts.
 *
 * <p>This annotation is used to identify methods that can handle completion requests for specific
 * prompts in the Model Context Protocol (MCP) server. When applied to a method, it indicates that
 * the method can provide auto-completion suggestions for the specified prompt.
 *
 * <p>Methods annotated with {@code @McpPromptCompletion} must:
 *
 * <ul>
 *   <li>Return {@link McpCompleteCompletion}
 *   <li>Accept exactly one parameter of type {@code McpSchema.CompleteRequest.CompleteArgument}
 *   <li>Be properly configured with a prompt name
 * </ul>
 *
 * <p>The annotation is retained at runtime and can only be applied to methods.
 *
 * @author codeboyzhou
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpPromptCompletion {
  /**
   * Specifies the name of the prompt for which completion is provided.
   *
   * <p>This value must match the name of an existing prompt that has been registered with the MCP
   * server. The completion method will be invoked when completion requests are made for this
   * specific prompt.
   *
   * <p>The name should be a non-empty string that follows the MCP naming conventions for prompts.
   *
   * @return the name of the prompt for which completion is provided
   */
  String name();

  /**
   * Specifies an optional title or description for the prompt completion.
   *
   * <p>This field provides a human-readable title or description for the completion functionality.
   * It can be used in documentation, UI displays, or logging to provide more context about what the
   * completion does.
   *
   * <p>If not specified, defaults to an empty string using {@link StringHelper#EMPTY}. This allows
   * the title to be optional while maintaining consistency across the codebase.
   *
   * @return the title or description for the prompt completion, defaults to empty string if not
   *     specified
   */
  String title() default StringHelper.EMPTY;
}
