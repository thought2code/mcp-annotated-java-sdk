package com.github.thought2code.mcp.annotated.annotation;

import com.github.thought2code.mcp.annotated.server.component.completion.McpCompleteCompletion;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for marking methods that provide completion functionality for MCP resources.
 *
 * <p>This annotation is used to identify methods that can handle completion requests for specific
 * resources in the Model Context Protocol (MCP) server. When applied to a method, it indicates that
 * the method can provide auto-completion suggestions for the specified resource URI.
 *
 * <p>Methods annotated with {@code @McpResourceCompletion} must:
 *
 * <ul>
 *   <li>Return {@link McpCompleteCompletion}
 *   <li>Accept exactly one parameter of type {@code McpSchema.CompleteRequest.CompleteArgument}
 *   <li>Be properly configured with a resource URI
 * </ul>
 *
 * <p>The annotation is retained at runtime and can only be applied to methods. Resource completion
 * is typically used to provide suggestions for resource identifiers, file paths, or other
 * resource-specific parameters.
 *
 * @author codeboyzhou
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpResourceCompletion {
  /**
   * Specifies the URI of the resource for which completion is provided.
   *
   * <p>This value must match the URI of an existing resource that has been registered with the MCP
   * server. The completion method will be invoked when completion requests are made for this
   * specific resource.
   *
   * <p>The URI string must be non-null and non-empty. It should uniquely identify the resource
   * within the MCP server context.
   *
   * @return the URI of the resource for which completion is provided
   */
  String uri();
}
