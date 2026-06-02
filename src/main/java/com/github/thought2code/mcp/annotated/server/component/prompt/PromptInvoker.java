package com.github.thought2code.mcp.annotated.server.component.prompt;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.server.component.Invocation;
import java.util.Map;

/**
 * Generated invocation contract for one {@code @McpPrompt} method.
 *
 * @author codeboyzhou
 */
@FunctionalInterface
public interface PromptInvoker {
  /**
   * Invokes one component prompt method using request arguments.
   *
   * @param context application context
   * @param arguments request arguments
   * @return invocation result
   */
  Invocation invoke(McpApplicationContext context, Map<String, Object> arguments);
}
