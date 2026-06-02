package com.github.thought2code.mcp.annotated.server.component.resource;

import io.modelcontextprotocol.spec.McpSchema;

/**
 * Immutable resource registration bundle produced at compile time.
 *
 * @param sourceMethod fully qualified source method id for logging and errors
 * @param resource MCP resource metadata (URI, MIME type, annotations)
 * @param invoker generated handler that invokes the annotated Java method
 * @author codeboyzhou
 */
public record ResourceDefinition(
    String sourceMethod, McpSchema.Resource resource, ResourceInvoker invoker) {}
