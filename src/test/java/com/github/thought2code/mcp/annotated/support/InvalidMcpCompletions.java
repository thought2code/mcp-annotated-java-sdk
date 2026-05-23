package com.github.thought2code.mcp.annotated.support;

import com.github.thought2code.mcp.annotated.annotation.McpPromptCompletion;
import com.github.thought2code.mcp.annotated.server.component.McpCompleteCompletion;
import io.modelcontextprotocol.spec.McpSchema;

/** Fixture with invalid completion method signatures for registration validation tests. */
public class InvalidMcpCompletions {

  @McpPromptCompletion(name = "badReturnType")
  public String badReturnType(McpSchema.CompleteRequest.CompleteArgument argument) {
    return "bad";
  }

  @McpPromptCompletion(name = "badParameterType")
  public McpCompleteCompletion badParameterType(String argument) {
    return McpCompleteCompletion.empty();
  }
}
