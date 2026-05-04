package com.github.thought2code.mcp.annotated.server.component;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpSyncServer;

/**
 * Strategy interface for registering MCP components with a server instance.
 *
 * <p>Implementations of this interface are responsible for discovering and registering a specific
 * type of MCP component (such as tools, resources, or prompts) with an {@link McpSyncServer} or
 * {@link McpAsyncServer}. Each implementation typically scans for methods annotated with the
 * corresponding MCP annotation and registers the discovered components accordingly.
 *
 * @see McpSyncServer
 * @see McpAsyncServer
 * @author codeboyzhou
 */
public interface McpComponentRegistrar {
  /**
   * Registers all discovered components of this type with the given synchronous MCP server.
   *
   * <p>This method scans for methods annotated with the appropriate annotation(s) for this
   * component type and registers them with the server. The exact discovery and registration
   * mechanism depends on the implementation.
   *
   * @param server the {@link McpSyncServer} instance to register the components with
   * @param context the application context for component discovery and localization
   */
  void register(McpSyncServer server, McpApplicationContext context);

  /**
   * Registers all discovered components of this type with the given asynchronous MCP server.
   *
   * <p>This method scans for methods annotated with the appropriate annotation(s) for this
   * component type and registers them with the server. The exact discovery and registration
   * mechanism depends on the implementation.
   *
   * @param server the {@link McpAsyncServer} instance to register the components with
   * @param context the application context for component discovery and localization
   */
  void register(McpAsyncServer server, McpApplicationContext context);
}
