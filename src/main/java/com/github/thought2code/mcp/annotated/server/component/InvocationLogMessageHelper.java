package com.github.thought2code.mcp.annotated.server.component;

import com.github.thought2code.mcp.annotated.enums.McpServerError;

/**
 * SLF4J message templates for generated component invoker failures.
 *
 * <p>Each template includes a {@code sourceMethod={}} placeholder filled by generated invokers when
 * logging {@link McpServerError#METHOD_INVOCATION_ERROR} scenarios.
 *
 * @author codeboyzhou
 */
public final class InvocationLogMessageHelper {

  private InvocationLogMessageHelper() {}

  /** Log template when a generated tool invoker throws. */
  public static final String TOOL_INVOCATION_FAILED = "Tool invocation failed for sourceMethod={}";

  /** Log template when a generated prompt invoker throws. */
  public static final String PROMPT_INVOCATION_FAILED =
      "Prompt invocation failed for sourceMethod={}";

  /** Log template when a generated resource invoker throws. */
  public static final String RESOURCE_INVOCATION_FAILED =
      "Resource invocation failed for sourceMethod={}";

  /** Log template when a generated completion invoker throws. */
  public static final String COMPLETION_INVOCATION_FAILED =
      "Completion invocation failed for sourceMethod={}";
}
