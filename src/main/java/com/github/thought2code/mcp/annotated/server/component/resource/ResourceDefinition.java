package com.github.thought2code.mcp.annotated.server.component.resource;

import io.modelcontextprotocol.spec.McpSchema;

/**
 * Build-time component MCP resource definition.
 *
 * @param sourceMethod source method descriptor used for diagnostics
 * @param resource MCP resource metadata
 * @param invoker generated resource invoker
 */
public record ResourceDefinition(
    String sourceMethod, McpSchema.Resource resource, ResourceInvoker invoker) {}
