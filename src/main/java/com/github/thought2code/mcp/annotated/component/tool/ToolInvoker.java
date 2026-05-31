package com.github.thought2code.mcp.annotated.component.tool;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.reflect.Invocation;
import java.util.Map;

/** Strongly-typed invocation contract generated at build time for one {@code @McpTool} method. */
@FunctionalInterface
public interface ToolInvoker {
  /**
   * Invokes one component tool method using request arguments.
   *
   * @param context application context
   * @param arguments request arguments
   * @return invocation result
   */
  Invocation invoke(McpApplicationContext context, Map<String, Object> arguments);
}
