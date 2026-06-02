package com.github.thought2code.mcp.annotated.server.component.prompt;

import io.modelcontextprotocol.spec.McpSchema;

/**
 * Immutable prompt registration bundle produced at compile time.
 *
 * @param sourceMethod fully qualified source method id for logging and errors
 * @param prompt MCP prompt metadata
 * @param description resolved prompt description returned in {@code GetPromptResult}
 * @param invoker generated handler that invokes the annotated Java method
 * @author codeboyzhou
 */
public record PromptDefinition(
    String sourceMethod, McpSchema.Prompt prompt, String description, PromptInvoker invoker) {}
