package com.github.thought2code.mcp.annotated.compiled.tool;

import io.modelcontextprotocol.spec.McpSchema;

/**
 * Build-time compiled MCP tool definition.
 *
 * @param sourceMethod source method descriptor used for diagnostics
 * @param tool MCP tool metadata
 * @param invoker generated tool invoker
 */
public record CompiledToolDefinition(
    String sourceMethod, McpSchema.Tool tool, CompiledToolInvoker invoker) {
}
