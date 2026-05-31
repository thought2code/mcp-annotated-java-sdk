package com.github.thought2code.mcp.annotated.component.prompt;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.reflect.Invocation;
import java.util.Map;

/** Strongly-typed invocation contract generated at build time for one {@code @McpPrompt} method. */
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
