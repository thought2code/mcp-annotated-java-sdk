package com.github.thought2code.mcp.annotated.server.component.tool;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * Immutable tool registration bundle produced at compile time.
 *
 * <p>Consumed by {@link ToolRegistration} to add {@link McpServerFeatures} tool specifications at
 * runtime.
 *
 * @param sourceMethod fully qualified source method id for logging and errors
 * @param tool MCP tool metadata (name, schemas, description)
 * @param invoker generated handler that invokes the annotated Java method
 * @author codeboyzhou
 */
public record ToolDefinition(String sourceMethod, McpSchema.Tool tool, ToolInvoker invoker) {}
