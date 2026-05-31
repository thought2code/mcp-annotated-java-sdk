package com.github.thought2code.mcp.annotated.server.component.completion;

import io.modelcontextprotocol.spec.McpSchema;

/**
 * Build-time component MCP completion definition.
 *
 * @param sourceMethod source method descriptor used for diagnostics
 * @param reference MCP completion reference
 * @param invoker generated completion invoker
 */
public record CompletionDefinition(
    String sourceMethod, McpSchema.CompleteReference reference, CompletionInvoker invoker) {}
