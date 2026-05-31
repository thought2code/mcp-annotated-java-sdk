package com.github.thought2code.mcp.annotated.server.component.spi;

import com.github.thought2code.mcp.annotated.server.component.completion.CompletionDefinition;
import com.github.thought2code.mcp.annotated.server.component.prompt.PromptDefinition;
import com.github.thought2code.mcp.annotated.server.component.resource.ResourceDefinition;
import com.github.thought2code.mcp.annotated.server.component.tool.ToolDefinition;
import java.util.List;

/**
 * Service-provider entry point for build-time generated MCP models.
 *
 * <p>The initial build-time pipeline generates {@code @McpTool} metadata and invocation bindings.
 */
public interface ComponentModelProvider {
  /**
   * Returns all component MCP tool definitions generated at build time.
   *
   * @return component tool definitions
   */
  default List<ToolDefinition> tools() {
    return List.of();
  }

  /**
   * Returns all component MCP prompt definitions generated at build time.
   *
   * @return component prompt definitions
   */
  default List<PromptDefinition> prompts() {
    return List.of();
  }

  /**
   * Returns all component MCP resource definitions generated at build time.
   *
   * @return component resource definitions
   */
  default List<ResourceDefinition> resources() {
    return List.of();
  }

  /**
   * Returns all component MCP completion definitions generated at build time.
   *
   * @return component completion definitions
   */
  default List<CompletionDefinition> completions() {
    return List.of();
  }
}
