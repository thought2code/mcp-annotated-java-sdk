package com.github.thought2code.mcp.annotated.server.component.resource;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.server.component.Invocation;

/**
 * Generated invocation contract for one {@code @McpResource} method.
 *
 * @author codeboyzhou
 */
@FunctionalInterface
public interface ResourceInvoker {
  /**
   * Invokes one component resource method.
   *
   * @param context application context
   * @return invocation result
   */
  Invocation invoke(McpApplicationContext context);
}
