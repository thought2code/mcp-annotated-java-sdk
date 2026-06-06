package com.github.thought2code.mcp.annotated.server;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.configuration.ServerConfiguration;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;

/**
 * MCP server implementation for Standard Input/Output (STDIO) mode.
 *
 * <p>This class extends {@link McpServerBase} and provides an MCP server implementation that uses
 * standard input/output for communication. STDIO mode is the default mode for CLI tools and allows
 * the server to communicate through standard input and output streams.
 *
 * <p>STDIO mode is suitable for:
 *
 * <ul>
 *   <li>Command-line interface (CLI) applications
 *   <li>Integration with shell scripts and pipelines
 *   <li>Simple communication scenarios where HTTP is not required
 * </ul>
 *
 * <p>The server uses the standard input stream for receiving requests and the standard output
 * stream for sending responses, making it easy to integrate with existing command-line tools.
 *
 * @author codeboyzhou
 * @see McpServerBase
 * @see McpStreamableServer
 * @see StdioServerTransportProvider
 */
public class McpStdioServer extends McpServerBase {
  /** STDIO transport whose session factory is activated in {@link #start()}. */
  private final DeferredActivationServerTransportProvider transportProvider;

  /** Keeps the main thread alive until {@link #stop()} is invoked. */
  private final CountDownLatch running = new CountDownLatch(1);

  /**
   * Constructs a new {@link McpStdioServer} with the specified configuration and application
   * context.
   *
   * @param configuration the server configuration
   * @param context the application-scoped runtime context
   */
  public McpStdioServer(ServerConfiguration configuration, McpApplicationContext context) {
    super(configuration, context);
    this.transportProvider =
        new DeferredActivationServerTransportProvider(
            new StdioServerTransportProvider(McpJsonDefaults.getMapper()));
  }

  /**
   * Creates and returns a synchronization specification for STDIO mode.
   *
   * <p>This method creates an {@link McpServer.SyncSpecification} from the shared STDIO transport
   * provider configured during server construction.
   *
   * @return a synchronization specification configured for STDIO transport
   * @see StdioServerTransportProvider
   * @see McpJsonDefaults
   */
  @Override
  public McpServer.SyncSpecification<?> createSyncSpecification() {
    return createSpecification(McpServer::sync);
  }

  /**
   * Creates and returns an asynchronous specification for STDIO mode.
   *
   * <p>This method creates an {@link McpServer.AsyncSpecification} from the same shared STDIO
   * transport provider used by the sync specification.
   *
   * @return an asynchronous specification configured for STDIO transport
   * @see StdioServerTransportProvider
   * @see McpJsonDefaults
   */
  @Override
  public McpServer.AsyncSpecification<?> createAsyncSpecification() {
    return createSpecification(McpServer::async);
  }

  /**
   * Creates a STDIO specification from the shared transport provider.
   *
   * @param factory factory that maps transport provider to server specification
   * @return created specification
   * @param <T> specification type
   */
  private <T> T createSpecification(
      Function<DeferredActivationServerTransportProvider, T> factory) {
    return factory.apply(transportProvider);
  }

  /**
   * Activates the STDIO transport after MCP components have been registered.
   *
   * @see DeferredActivationServerTransportProvider#activate()
   */
  @Override
  public void start() {
    transportProvider.activate();
  }

  /**
   * Blocks until {@link #stop()} is called.
   *
   * <p>STDIO servers must keep the main thread alive so the subprocess or CLI entrypoint does not
   * exit before the transport finishes handling requests.
   */
  @Override
  public void awaitShutdown() {
    try {
      running.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Releases the main-thread latch and closes the STDIO transport.
   *
   * <p>Unblocks {@link #awaitShutdown()} and gracefully shuts down the underlying transport
   * provider.
   */
  @Override
  public void stop() {
    running.countDown();
    transportProvider.closeGracefully().block();
  }
}
