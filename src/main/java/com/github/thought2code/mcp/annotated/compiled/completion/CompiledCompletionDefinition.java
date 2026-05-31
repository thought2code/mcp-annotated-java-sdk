package com.github.thought2code.mcp.annotated.compiled.completion;

import io.modelcontextprotocol.spec.McpSchema;

/**
 * Build-time compiled MCP completion definition.
 *
 * @param sourceMethod source method descriptor used for diagnostics
 * @param reference MCP completion reference
 * @param invoker generated completion invoker
 */
public record CompiledCompletionDefinition(
    String sourceMethod, McpSchema.CompleteReference reference, CompiledCompletionInvoker invoker) {
}
