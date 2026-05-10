package com.github.thought2code.mcp.annotated.server;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.configuration.McpServerConfiguration;
import com.github.thought2code.mcp.annotated.configuration.McpServerSSE;
import com.github.thought2code.mcp.annotated.enums.ServerMode;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MCP server implementation for HTTP Server-Sent Events (SSE) mode.
 *
 * <p>This class extends {@link McpServerBase} and provides an MCP server implementation that uses
 * Server-Sent Events for HTTP-based real-time communication. SSE mode allows the server to push
 * updates to clients over a single HTTP connection.
 *
 * <p>Note: This mode has been deprecated in favor of STREAMABLE mode. Consider using {@link
 * McpStreamableServer} for new implementations.
 *
 * <p>The server uses Jetty HTTP server for handling HTTP connections and provides SSE endpoints for
 * real-time communication with MCP clients.
 *
 * @author codeboyzhou
 * @see McpServerBase
 * @see McpStreamableServer
 * @see McpStdioServer
 * @see HttpServletSseServerTransportProvider
 * @see JettyHttpServer
 */
public class McpSseServer extends McpServerBase {

  private static final Logger log = LoggerFactory.getLogger(McpSseServer.class);

  /** The HTTP SSE server transport provider used by this MCP server. */
  private final HttpServletSseServerTransportProvider transportProvider;

  /** The port number used by this MCP server. */
  private final int port;

  /**
   * Constructs a new {@link McpSseServer} with the specified configuration and application context.
   *
   * @param configuration the server configuration containing SSE settings
   * @param context the application-scoped runtime context
   */
  public McpSseServer(McpServerConfiguration configuration, McpApplicationContext context) {
    super(configuration, context);
    McpServerSSE sse = configuration.sse();
    this.port = sse.port();
    this.transportProvider =
        HttpServletSseServerTransportProvider.builder()
            .jsonMapper(McpJsonDefaults.getMapper())
            .baseUrl(sse.baseUrl())
            .sseEndpoint(sse.endpoint())
            .messageEndpoint(sse.messageEndpoint())
            .build();
  }

  /**
   * Creates and returns a synchronization specification for SSE mode.
   *
   * <p>This method creates an {@link McpServer.SyncSpecification} from the shared SSE transport
   * provider configured during server construction.
   *
   * <p>Note: This method logs a deprecation warning as SSE mode is deprecated in favor of
   * STREAMABLE mode.
   *
   * @return a synchronization specification configured for SSE transport
   * @see HttpServletSseServerTransportProvider
   * @see McpServerSSE
   * @see McpJsonDefaults
   */
  @Override
  public McpServer.SyncSpecification<?> createSyncSpecification() {
    return createSpecification(McpServer::sync);
  }

  /**
   * Creates and returns an asynchronous specification for SSE mode.
   *
   * <p>This method creates an {@link McpServer.AsyncSpecification} from the same shared SSE
   * transport provider used by the sync specification.
   *
   * @return an asynchronous specification configured for SSE transport
   * @see HttpServletSseServerTransportProvider
   */
  @Override
  public McpServer.AsyncSpecification<?> createAsyncSpecification() {
    return createSpecification(McpServer::async);
  }

  /**
   * Creates an SSE specification while keeping deprecation warning behavior consistent.
   *
   * @param factory factory that maps transport provider to server specification
   * @return created specification
   * @param <T> specification type
   */
  private <T> T createSpecification(Function<HttpServletSseServerTransportProvider, T> factory) {
    log.warn("HTTP SSE mode has been deprecated, recommend to use Stream HTTP server instead.");
    return factory.apply(transportProvider);
  }

  /**
   * Starts the SSE HTTP server to accept incoming connections.
   *
   * <p>This method creates a Jetty HTTP server configured with the SSE transport provider and binds
   * it to the configured port. The server begins accepting connections immediately.
   *
   * @see JettyHttpServer
   * @see HttpServletSseServerTransportProvider
   */
  @Override
  public void start() {
    final String endpointPath = configuration.sse().endpoint();
    startHttpServer(ServerMode.SSE, endpointPath, transportProvider, port);
  }
}
