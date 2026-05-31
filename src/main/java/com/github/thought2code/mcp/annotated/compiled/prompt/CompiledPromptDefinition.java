package com.github.thought2code.mcp.annotated.compiled.prompt;

import io.modelcontextprotocol.spec.McpSchema;

/**
 * Build-time compiled MCP prompt definition.
 *
 * @param sourceMethod source method descriptor used for diagnostics
 * @param prompt MCP prompt metadata
 * @param description resolved prompt description
 * @param invoker generated prompt invoker
 */
public record CompiledPromptDefinition(
    String sourceMethod,
    McpSchema.Prompt prompt,
    String description,
    CompiledPromptInvoker invoker) {

}
