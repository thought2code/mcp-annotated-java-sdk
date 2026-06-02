package com.github.thought2code.mcp.annotated.server.component.completion;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.server.component.Invocation;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * Generated invocation contract for one completion method.
 *
 * @author codeboyzhou
 */
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
