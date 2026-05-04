package com.github.thought2code.mcp.annotated.server;

import com.github.thought2code.mcp.annotated.configuration.McpServerDefaults;
import jakarta.servlet.http.HttpServlet;
import java.util.Objects;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple Jetty HTTP server implementation.
 *
 * @author codeboyzhou
 */
public class JettyHttpServer {

  private static final Logger log = LoggerFactory.getLogger(JettyHttpServer.class);

  /** Jetty thread pool name. */
  private static final String JETTY_THREAD_POOL_NAME = "jetty-based-mcp-server-worker";

  /** Default context path. */
  private static final String DEFAULT_CONTEXT_PATH = "/";

  /** Default servlet path. */
  private static final String DEFAULT_SERVLET_PATH = "/*";

  /** MCP transport provider to be registered in Jetty HTTP server. */
  private HttpServlet mcpTransportProvider;

  /** Port to bind Jetty HTTP server. */
  private int port = McpServerDefaults.PORT;

  /** Jetty server instance. */
  private Server server;

  /**
   * Register a servlet to be handled by Jetty HTTP server.
   *
   * @param mcpTransportProvider the MCP transport provider to be registered
   * @return this server instance
   */
  public JettyHttpServer withTransportProvider(@NotNull HttpServlet mcpTransportProvider) {
    this.mcpTransportProvider = mcpTransportProvider;
    return this;
  }

  /**
   * Bind Jetty HTTP server to a specific port.
   *
   * @param port the port to bind the server to
   * @return this server instance
   */
  public JettyHttpServer bind(int port) {
    if (port <= 0 || port > 65535) {
      throw new IllegalArgumentException("Port must be between 1 and 65535");
    }
    this.port = port;
    return this;
  }

  /** Start Jetty HTTP server and bind it to the specified port. */
  public void start() {
    if (server != null && server.isRunning()) {
      log.warn("Jetty-based MCP server is already started");
      return;
    }

    try {
      initServer();
      server.start();
      log.info("Jetty-based MCP server started successfully");

      final boolean isTesting = Boolean.parseBoolean(System.getProperty("mcp.server.testing"));
      if (isTesting) {
        log.debug("Testing Jetty-based MCP server, not awaiting for server to stop");
        return;
      }

      await(server);
    } catch (Exception e) {
      log.error("Error starting Jetty-based MCP server", e);
      stop();
      throw new IllegalStateException("Failed to start Jetty-based MCP server on port " + port, e);
    }
  }

  /** Initialize Jetty HTTP server instance. */
  private void initServer() {
    Objects.requireNonNull(mcpTransportProvider, "mcpTransportProvider must not be null");

    QueuedThreadPool threadPool = new QueuedThreadPool();
    threadPool.setName(JETTY_THREAD_POOL_NAME);
    server = new Server(threadPool);
    server.setStopAtShutdown(true);

    ServerConnector connector = new ServerConnector(server);
    connector.setPort(port);
    server.addConnector(connector);

    ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.SESSIONS);
    handler.setContextPath(DEFAULT_CONTEXT_PATH);
    handler.addServlet(new ServletHolder(mcpTransportProvider), DEFAULT_SERVLET_PATH);
    server.setHandler(handler);
  }

  /**
   * Await for Jetty HTTP server to stop.
   *
   * @param server the Jetty HTTP server instance
   */
  private void await(Server server) {
    try {
      server.join();
    } catch (InterruptedException e) {
      log.error("Error joining Jetty-based MCP server", e);
    }
  }

  /** Stop Jetty HTTP server. */
  public void stop() {
    if (server != null) {
      try {
        if (server.isRunning()) {
          server.stop();
          log.info("Jetty-based MCP server stopped");
        }
      } catch (Exception e) {
        log.error("Error stopping Jetty-based MCP server", e);
      } finally {
        server.destroy();
        server = null;
      }
    }
  }
}
