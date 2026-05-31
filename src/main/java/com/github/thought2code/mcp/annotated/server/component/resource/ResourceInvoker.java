package com.github.thought2code.mcp.annotated.server.component.resource;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.server.component.Invocation;

/**
 * Strongly-typed invocation contract generated at build time for one {@code @McpResource} method.
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
