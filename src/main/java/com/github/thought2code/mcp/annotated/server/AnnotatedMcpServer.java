package com.github.thought2code.mcp.annotated.server;

import com.github.thought2code.mcp.annotated.McpApplication;
import com.github.thought2code.mcp.annotated.enums.ServerMode;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * SDK-level contract for an MCP server runtime (transport + lifecycle), distinct from {@link
 * McpServer} which builds sync/async specifications from the MCP Java SDK.
 *
 * <p>Implementations cover server modes such as {@link ServerMode#STDIO}, {@link ServerMode#SSE},
 * and {@link ServerMode#STREAMABLE}. Typical usage is through {@link McpServerBase} subclasses
 * together with {@link McpApplication}.
 *
 * @author codeboyzhou
 * @see McpServerBase
 * @see McpStdioServer
 * @see McpSseServer
 * @see McpStreamableServer
 */
public interface AnnotatedMcpServer {
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
   * @see McpServer.SyncSpecification
   */
  McpServer.SyncSpecification<?> createSyncSpecification();

  /**
   * Creates and returns the asynchronous specification for this MCP server.
   *
   * <p>The async specification contains the transport provider and other configuration details
   * needed to create an asynchronous MCP server instance.
   *
   * @return the asynchronous specification for the server
   * @see McpServer.AsyncSpecification
   */
  McpServer.AsyncSpecification<?> createAsyncSpecification();

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

  /**
   * Stops the MCP server and releases transport resources.
   *
   * <p>For HTTP-based server modes (SSE, STREAMABLE), this method stops the underlying Jetty
   * server. For STDIO mode, this method is a no-op.
   */
  default void stop() {}

  /**
   * Blocks until the server shuts down.
   *
   * <p>HTTP-based implementations delegate to the underlying Jetty server. STDIO mode uses the
   * default no-op because the process lifecycle is managed by the MCP transport.
   */
  default void awaitShutdown() {}
}
