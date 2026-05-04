package com.github.thought2code.mcp.annotated.server;

import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * Interface representing a Model Context Protocol (MCP) server that provides the core functionality
 * for MCP server implementations.
 *
 * <p>This interface defines the contract for MCP servers, including capability definition, server
 * specification creation, server instantiation, and component registration. Implementations of this
 * interface should provide the specific behavior for different server modes (STDIO, SSE,
 * STREAMABLE).
 *
 * <p>Typical usage involves implementing this interface for a specific server mode and providing
 * the necessary configuration and component registration logic.
 *
 * @author codeboyzhou
 * @see McpServerBase
 * @see McpStdioServer
 * @see McpSseServer
 * @see McpStreamableServer
 */
public interface McpServer {
  /**
   * Defines and returns the server capabilities that this MCP server supports.
   *
   * <p>Capabilities include support for resources, prompts, tools, and completions, as well as
   * change notification settings for each component type.
   *
   * @return the server capabilities configuration
   * @see McpSchema.ServerCapabilities
   */
  McpSchema.ServerCapabilities defineCapabilities();

  /**
   * Creates and returns the synchronization specification for this MCP server.
   *
   * <p>The sync specification contains the transport provider and other configuration details
   * needed to create a synchronized MCP server instance.
   *
   * @return the synchronization specification for the server
   * @see io.modelcontextprotocol.server.McpServer.SyncSpecification
   */
  io.modelcontextprotocol.server.McpServer.SyncSpecification<?> createSyncSpecification();

  /**
   * Creates and returns the asynchronous specification for this MCP server.
   *
   * <p>The async specification contains the transport provider and other configuration details
   * needed to create an asynchronous MCP server instance.
   *
   * @return the asynchronous specification for the server
   * @see io.modelcontextprotocol.server.McpServer.AsyncSpecification
   */
  io.modelcontextprotocol.server.McpServer.AsyncSpecification<?> createAsyncSpecification();

  /**
   * Creates and returns a fully configured MCP synchronous server instance.
   *
   * <p>This method should create a server instance with all necessary configurations applied,
   * including server info, capabilities, and transport settings.
   *
   * @return a fully configured MCP synchronous server
   * @see McpSyncServer
   */
  McpSyncServer createSyncServer();

  /**
   * Creates and returns a fully configured MCP asynchronous server instance.
   *
   * <p>This method should create a server instance with all necessary configurations applied,
   * including server info, capabilities, and transport settings.
   *
   * @return a fully configured MCP asynchronous server
   * @see McpAsyncServer
   */
  McpAsyncServer createAsyncServer();

  /**
   * Registers all MCP server components (resources, prompts, tools) with the specified synchronous
   * server instance.
   *
   * <p>This method should scan for annotated methods and register them as appropriate MCP
   * components with the server. Components are discovered using reflection and registered through
   * the component registry.
   *
   * @param mcpSyncServer the synchronous server instance to register components with
   */
  void registerComponents(McpSyncServer mcpSyncServer);

  /**
   * Registers all MCP server components (resources, prompts, tools) with the specified asynchronous
   * server instance.
   *
   * <p>This method should scan for annotated methods and register them as appropriate MCP
   * components with the server. Components are discovered using reflection and registered through
   * the component registry.
   *
   * @param mcpAsyncServer the asynchronous server instance to register components with
   */
  void registerComponents(McpAsyncServer mcpAsyncServer);

  /**
   * Starts the MCP server.
   *
   * <p>For HTTP-based server modes (SSE, STREAMABLE), this method starts the underlying HTTP server
   * to accept incoming connections. For STDIO mode, this method is a no-op since the transport is
   * tied to standard input/output and starts automatically when the sync server is created.
   *
   * <p>Implementations that require explicit startup (e.g., HTTP servers) should override this
   * method to start their transport layer. The default implementation does nothing.
   */
  default void start() {}
}
