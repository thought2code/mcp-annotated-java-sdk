package com.github.thought2code.mcp.annotated.server.component.tool;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.server.component.Invocation;
import java.util.Map;

/**
 * Generated invocation contract for one {@code @McpTool} method.
 *
 * @author codeboyzhou
 */
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
