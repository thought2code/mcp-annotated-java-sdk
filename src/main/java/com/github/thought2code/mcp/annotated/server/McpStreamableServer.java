package com.github.thought2code.mcp.annotated.server;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.configuration.McpServerConfiguration;
import com.github.thought2code.mcp.annotated.configuration.McpServerStreamable;
import com.github.thought2code.mcp.annotated.util.InetHelper;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * @see McpServerConfiguration
 * @see McpServerStreamable
 * @see HttpServletStreamableServerTransportProvider
 * @see JettyHttpServer
 */
public class McpStreamableServer extends McpServerBase {

  private static final Logger log = LoggerFactory.getLogger(McpStreamableServer.class);

  /** The HTTP Streamable server transport provider used by this MCP server. */
  private HttpServletStreamableServerTransportProvider transportProvider;

  /** The port number on which this MCP server listens for incoming connections. */
  private int port;

  /**
   * Constructs a new {@link McpStreamableServer} with the specified configuration and application
   * context.
   *
   * @param configuration the server configuration containing streamable settings
   * @param context the application-scoped runtime context
   */
  public McpStreamableServer(McpServerConfiguration configuration, McpApplicationContext context) {
    super(configuration, context);
  }

  /**
   * Creates and returns a synchronization specification for Streamable HTTP mode.
   *
   * <p>This method creates an {@link McpServer.SyncSpecification} that uses HTTP streaming
   * transport provider for communication. The transport provider is configured with the following
   * settings from the configuration:
   *
   * <ul>
   *   <li>Port number for binding the HTTP server
   *   <li>MCP endpoint path for the streaming API
   *   <li>Whether to disallow delete operations
   *   <li>Keep-alive interval for maintaining connections
   * </ul>
   *
   * <p>The method also stores the port number and transport provider instance for later use when
   * starting the HTTP server.
   *
   * @return a synchronization specification configured for HTTP streaming transport
   * @see HttpServletStreamableServerTransportProvider
   * @see McpServerStreamable
   * @see McpJsonDefaults
   */
  @Override
  public McpServer.SyncSpecification<?> createSyncSpecification() {
    McpServerStreamable streamable = configuration.streamable();
    port = streamable.port();
    transportProvider =
        HttpServletStreamableServerTransportProvider.builder()
            .jsonMapper(McpJsonDefaults.getMapper())
            .mcpEndpoint(streamable.mcpEndpoint())
            .disallowDelete(streamable.disallowDelete())
            .keepAliveInterval(Duration.ofMillis(streamable.keepAliveInterval()))
            .build();
    return McpServer.sync(transportProvider);
  }

  /**
   * Creates and returns an asynchronous specification for Streamable HTTP mode.
   *
   * <p>This method creates an {@link McpServer.AsyncSpecification} that uses HTTP streaming
   * transport provider for communication. It reuses the same transport provider configuration as
   * the sync specification.
   *
   * @return an asynchronous specification configured for HTTP streaming transport
   * @see HttpServletStreamableServerTransportProvider
   */
  @Override
  public McpServer.AsyncSpecification<?> createAsyncSpecification() {
    McpServerStreamable streamable = configuration.streamable();
    port = streamable.port();
    transportProvider =
        HttpServletStreamableServerTransportProvider.builder()
            .jsonMapper(McpJsonDefaults.getMapper())
            .mcpEndpoint(streamable.mcpEndpoint())
            .disallowDelete(streamable.disallowDelete())
            .keepAliveInterval(Duration.ofMillis(streamable.keepAliveInterval()))
            .build();
    return McpServer.async(transportProvider);
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
    log.info(
        "Starting Jetty-based MCP Streamable server on http://{}:{}{}",
        InetHelper.findFirstNonLoopbackAddress().getHostAddress(),
        configuration.streamable().port(),
        configuration.streamable().mcpEndpoint());
    JettyHttpServer httpServer = new JettyHttpServer();
    httpServer.withTransportProvider(transportProvider).bind(port).start();
  }
}
