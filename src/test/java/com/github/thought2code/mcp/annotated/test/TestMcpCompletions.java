package com.github.thought2code.mcp.annotated.test;

import com.github.thought2code.mcp.annotated.annotation.McpPromptCompletion;
import com.github.thought2code.mcp.annotated.annotation.McpResourceCompletion;
import com.github.thought2code.mcp.annotated.server.component.completion.CompletionResult;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;

/** Fixture completions registered within the integration test base package scope. */
public class TestMcpCompletions {

  @McpPromptCompletion(name = "generateCode", title = "Code languages")
  public CompletionResult completeGenerateCode(
      McpSchema.CompleteRequest.CompleteArgument argument) {
    return CompletionResult.builder()
        .values(List.of("Java", "Python"))
        .total(2)
        .hasMore(false)
        .build();
  }

  @McpResourceCompletion(uri = "file://{path}")
  public CompletionResult completeFileUri(McpSchema.CompleteRequest.CompleteArgument argument) {
    return CompletionResult.builder()
        .values(List.of("file://a", "file://b"))
        .total(2)
        .hasMore(true)
        .build();
  }
}
