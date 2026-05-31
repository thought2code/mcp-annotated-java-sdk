package com.github.thought2code.mcp.annotated.server.component.tool;

import io.modelcontextprotocol.spec.McpSchema;

/**
 * Build-time component MCP tool definition.
 *
 * @param sourceMethod source method descriptor used for diagnostics
 * @param tool MCP tool metadata
 * @param invoker generated tool invoker
 */
public record ToolDefinition(String sourceMethod, McpSchema.Tool tool, ToolInvoker invoker) {}
