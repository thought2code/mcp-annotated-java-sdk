package com.github.thought2code.mcp.annotated.support;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.configuration.McpConfigurationLoader;
import com.github.thought2code.mcp.annotated.configuration.McpServerConfiguration;
import com.github.thought2code.mcp.annotated.enums.ServerType;
import com.github.thought2code.mcp.annotated.server.AnnotatedMcpServer;
import com.github.thought2code.mcp.annotated.server.McpSseServer;
import com.github.thought2code.mcp.annotated.server.McpStdioServer;
import com.github.thought2code.mcp.annotated.server.McpStreamableServer;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpSyncServer;
import java.util.Objects;

/**
 * Test helper that mirrors {@link com.github.thought2code.mcp.annotated.McpApplication} startup
 * while returning the started {@link AnnotatedMcpServer} so tests can call {@link
 * AnnotatedMcpServer#stop()}.
 */
public final class TestMcpServerLifecycle {

  private TestMcpServerLifecycle() {
    throw new UnsupportedOperationException("Utility class should not be instantiated");
  }

  /**
   * Loads configuration and starts an MCP server for tests.
   *
   * @param context application context
   * @param configFileName classpath configuration file
   * @return started server, or {@code null} when the configuration is disabled
   */
  public static AnnotatedMcpServer start(McpApplicationContext context, String configFileName) {
    McpServerConfiguration configuration = new McpConfigurationLoader(configFileName).loadConfig();
    return start(context, configuration);
  }

  /**
   * Starts an MCP server from the given configuration for tests.
   *
   * @param context application context
   * @param configuration server configuration
   * @return started server, or {@code null} when the configuration is disabled
   */
  @SuppressWarnings("deprecation")
  public static AnnotatedMcpServer start(
      McpApplicationContext context, McpServerConfiguration configuration) {
    if (!configuration.enabled()) {
      return null;
    }

    AnnotatedMcpServer mcpServer =
        switch (configuration.mode()) {
          case STDIO -> new McpStdioServer(configuration, context);
          case SSE -> new McpSseServer(configuration, context);
          case STREAMABLE -> new McpStreamableServer(configuration, context);
        };

    Objects.requireNonNull(mcpServer, "mcpServer must not be null");

    ServerType serverType = configuration.type();
    switch (serverType) {
      case SYNC -> {
        McpSyncServer mcpSyncServer = mcpServer.createSyncServer();
        mcpServer.registerComponents(mcpSyncServer);
      }
      case ASYNC -> {
        McpAsyncServer mcpAsyncServer = mcpServer.createAsyncServer();
        mcpServer.registerComponents(mcpAsyncServer);
      }
    }

    mcpServer.start();
    return mcpServer;
  }
}
