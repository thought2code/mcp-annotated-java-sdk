package com.github.thought2code.mcp.annotated.server.component;

/** Shared invocation failure log-message templates for generated component invokers. */
public final class InvocationLogMessageHelper {

  private InvocationLogMessageHelper() {}

  public static final String TOOL_INVOCATION_FAILED = "Tool invocation failed for sourceMethod={}";

  public static final String PROMPT_INVOCATION_FAILED =
      "Prompt invocation failed for sourceMethod={}";

  public static final String RESOURCE_INVOCATION_FAILED =
      "Resource invocation failed for sourceMethod={}";

  public static final String COMPLETION_INVOCATION_FAILED =
      "Completion invocation failed for sourceMethod={}";
}
