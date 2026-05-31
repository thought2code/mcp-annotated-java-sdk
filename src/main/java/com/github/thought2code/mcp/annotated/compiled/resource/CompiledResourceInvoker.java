package com.github.thought2code.mcp.annotated.compiled.resource;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.reflect.Invocation;

/**
 * Strongly-typed invocation contract generated at build time for one {@code @McpResource} method.
 */
@FunctionalInterface
public interface CompiledResourceInvoker {
  /**
   * Invokes one compiled resource method.
   *
   * @param context application context
   * @return invocation result
   */
  Invocation invoke(McpApplicationContext context);
}
