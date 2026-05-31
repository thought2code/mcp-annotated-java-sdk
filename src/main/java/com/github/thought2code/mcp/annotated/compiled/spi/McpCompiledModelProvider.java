package com.github.thought2code.mcp.annotated.compiled.spi;

import com.github.thought2code.mcp.annotated.compiled.completion.CompiledCompletionDefinition;
import com.github.thought2code.mcp.annotated.compiled.prompt.CompiledPromptDefinition;
import com.github.thought2code.mcp.annotated.compiled.resource.CompiledResourceDefinition;
import com.github.thought2code.mcp.annotated.compiled.tool.CompiledToolDefinition;
import java.util.List;

/**
 * Service-provider entry point for build-time generated MCP models.
 *
 * <p>The initial build-time pipeline compiles {@code @McpTool} metadata and invocation bindings.
 */
public interface McpCompiledModelProvider {
  /**
   * Returns all compiled MCP tool definitions generated at build time.
   *
   * @return compiled tool definitions
   */
  default List<CompiledToolDefinition> tools() {
    return List.of();
  }

  /**
   * Returns all compiled MCP prompt definitions generated at build time.
   *
   * @return compiled prompt definitions
   */
  default List<CompiledPromptDefinition> prompts() {
    return List.of();
  }

  /**
   * Returns all compiled MCP resource definitions generated at build time.
   *
   * @return compiled resource definitions
   */
  default List<CompiledResourceDefinition> resources() {
    return List.of();
  }

  /**
   * Returns all compiled MCP completion definitions generated at build time.
   *
   * @return compiled completion definitions
   */
  default List<CompiledCompletionDefinition> completions() {
    return List.of();
  }
}
