package com.github.thought2code.mcp.annotated.component.prompt;

import io.modelcontextprotocol.spec.McpSchema;

/**
 * Build-time component MCP prompt definition.
 *
 * @param sourceMethod source method descriptor used for diagnostics
 * @param prompt MCP prompt metadata
 * @param description resolved prompt description
 * @param invoker generated prompt invoker
 */
public record PromptDefinition(
    String sourceMethod, McpSchema.Prompt prompt, String description, PromptInvoker invoker) {}
