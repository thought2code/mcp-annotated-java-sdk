package com.github.thought2code.mcp.annotated.server.component.completion;

import io.modelcontextprotocol.spec.McpSchema;

/**
 * Immutable completion registration bundle produced at compile time.
 *
 * @param sourceMethod fully qualified source method id for logging and errors
 * @param reference prompt or resource reference this completion augments
 * @param invoker generated handler that invokes the annotated Java method
 * @author codeboyzhou
 */
public record CompletionDefinition(
    String sourceMethod, McpSchema.CompleteReference reference, CompletionInvoker invoker) {}
