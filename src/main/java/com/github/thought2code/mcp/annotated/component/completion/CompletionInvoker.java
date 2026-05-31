package com.github.thought2code.mcp.annotated.component.completion;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.reflect.Invocation;
import io.modelcontextprotocol.spec.McpSchema;

/** Strongly-typed invocation contract generated at build time for one completion method. */
@FunctionalInterface
public interface CompletionInvoker {
  /**
   * Invokes one component completion method.
   *
   * @param context application context
   * @param argument completion argument from request
   * @return invocation result
   */
  Invocation invoke(
      McpApplicationContext context, McpSchema.CompleteRequest.CompleteArgument argument);
}
