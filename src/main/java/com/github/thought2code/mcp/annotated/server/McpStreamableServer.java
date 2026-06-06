package com.github.thought2code.mcp.annotated.server;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.configuration.ServerConfiguration;
import com.github.thought2code.mcp.annotated.configuration.ServerStreamable;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import java.time.Duration;
import java.util.function.Function;

/**
 * An MCP server implementation that operates in Streamable HTTP mode.
 *
 * <p>This class extends {@link McpServerBase} and provides functionality for creating and managing
 * MCP servers that use HTTP streaming for communication. The streamable mode is designed for web
 * applications that require real-time, bidirectional communication with the MCP server.
 *
 * <p>The server uses Jetty as the underlying HTTP server and supports various configuration options
 * including:
 *
 * <ul>
 *   <li>Custom port binding
 *   <li>Configurable MCP endpoint path
 *   <li>Keep-alive interval management
 *   <li>Delete operation control
 * </ul>
 *
 * <p>This server mode is particularly suitable for:
 *
 * <ul>
 *   <li>Web applications requiring real-time communication
 *   <li>Browser-based MCP client integrations
 *   <li>Scenarios needing HTTP-based streaming communication
 * </ul>
 *
 * @author codeboyzhou
 * @see McpServerBase
 * @see ServerConfiguration
 * @see ServerStreamable
 * @see HttpServletStreamableServerTransportProvider
 * @see JettyHttpServer
 */
public class McpStreamableServer extends McpServerBase {

  /** The HTTP Streamable server transport provider used by this MCP server. */
  private final HttpServletStreamableServerTransportProvider transportProvider;

  /** The port number on which this MCP server listens for incoming connections. */
  private final int port;

  /**
   * Constructs a new {@link McpStreamableServer} with the specified configuration and application
   * context.
   *
   * @param configuration the server configuration containing streamable settings
   * @param context the application-scoped runtime context
   */
  public McpStreamableServer(ServerConfiguration configuration, McpApplicationContext context) {
    super(configuration, context);
    ServerStreamable streamable = configuration.streamable();
    this.port = streamable.port();
    this.transportProvider =
        HttpServletStreamableServerTransportProvider.builder()
            .jsonMapper(McpJsonDefaults.getMapper())
            .mcpEndpoint(streamable.mcpEndpoint())
            .disallowDelete(streamable.disallowDelete())
            .keepAliveInterval(Duration.ofMillis(streamable.keepAliveInterval()))
            .build();
  }

  /**
   * Creates and returns a synchronization specification for Streamable HTTP mode.
   *
   * <p>This method creates an {@link McpServer.SyncSpecification} from the shared Streamable
   * transport provider configured during server construction.
   *
   * @return a synchronization specification configured for HTTP streaming transport
   * @see HttpServletStreamableServerTransportProvider
   * @see ServerStreamable
   * @see McpJsonDefaults
   */
  @Override
  public McpServer.SyncSpecification<?> createSyncSpecification() {
    return createSpecification(McpServer::sync);
  }

  /**
   * Creates and returns an asynchronous specification for Streamable HTTP mode.
   *
   * <p>This method creates an {@link McpServer.AsyncSpecification} from the same shared Streamable
   * transport provider used by the sync specification.
   *
   * @return an asynchronous specification configured for HTTP streaming transport
   * @see HttpServletStreamableServerTransportProvider
   */
  @Override
  public McpServer.AsyncSpecification<?> createAsyncSpecification() {
    return createSpecification(McpServer::async);
  }

  /**
   * Creates a Streamable specification from the shared transport provider.
   *
   * @param factory factory that maps transport provider to server specification
   * @return created specification
   * @param <T> specification type
   */
  private <T> T createSpecification(
      Function<HttpServletStreamableServerTransportProvider, T> factory) {
    return factory.apply(transportProvider);
  }

  /**
   * Starts the Streamable HTTP server to accept incoming connections.
   *
   * <p>This method creates a Jetty HTTP server configured with the streamable transport provider
   * and binds it to the configured port. The server begins accepting connections immediately.
   *
   * @see JettyHttpServer
   * @see HttpServletStreamableServerTransportProvider
   */
  @Override
  public void start() {
    final String mcpEndpoint = configuration.streamable().mcpEndpoint();
    startHttpServer(mcpEndpoint, transportProvider, port);
  }
}
