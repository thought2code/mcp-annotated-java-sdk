package com.github.thought2code.mcp.annotated;

import com.github.thought2code.mcp.annotated.configuration.McpConfigurationLoader;
import com.github.thought2code.mcp.annotated.configuration.McpServerConfiguration;
import com.github.thought2code.mcp.annotated.configuration.McpServerDefaults;
import com.github.thought2code.mcp.annotated.enums.ServerMode;
import com.github.thought2code.mcp.annotated.enums.ServerType;
import com.github.thought2code.mcp.annotated.server.AnnotatedMcpServer;
import com.github.thought2code.mcp.annotated.server.McpSseServer;
import com.github.thought2code.mcp.annotated.server.McpStdioServer;
import com.github.thought2code.mcp.annotated.server.McpStreamableServer;
import com.github.thought2code.mcp.annotated.util.JacksonHelper;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpSyncServer;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main application class for the MCP (Model Context Protocol) annotated server.
 *
 * <p>This class provides the entry point for running MCP servers with annotation-based
 * configuration. It supports STDIO and STREAMABLE modes. {@link ServerMode#SSE} is deprecated but
 * still supported for backward compatibility. The application automatically loads configuration,
 * initializes reflection scanning, and starts the appropriate server based on the configuration
 * settings.
 *
 * @author codeboyzhou
 */
@SuppressWarnings("unused")
public final class McpApplication {

  private static final Logger log = LoggerFactory.getLogger(McpApplication.class);

  private McpApplication() {
    throw new UnsupportedOperationException("Main application class should not be instantiated");
  }

  /**
   * Runs the MCP application with the specified main class and command-line arguments.
   *
   * <p>This method initializes the reflection provider, loads the resource bundle, and starts the
   * MCP server based on the configuration settings.
   *
   * @param mainClass the main class of the application, used as the base for reflection scanning
   * @param args the command-line arguments passed to the application
   * @param configFileName the name of the configuration file to load
   * @see McpApplicationContext
   */
  @SuppressWarnings("unused")
  public static void run(Class<?> mainClass, String[] args, String configFileName) {
    log.info("Running {} with args: {}", mainClass.getSimpleName(), args);
    McpApplicationContext context = McpApplicationContext.from(mainClass);
    startMcpServer(configFileName, context);
  }

  /**
   * Runs the MCP application with the specified main class and command-line arguments using the
   * default configuration file.
   *
   * <p>This method initializes the reflection provider, loads the resource bundle, and starts the
   * MCP server based on the configuration settings.
   *
   * @param mainClass the main class of the application, used as the base for reflection scanning
   * @param args the command-line arguments passed to the application
   */
  public static void run(Class<?> mainClass, String[] args) {
    run(mainClass, args, McpServerDefaults.CONFIG_FILE_NAME);
  }

  /**
   * Starts the MCP server based on the loaded configuration.
   *
   * <p>This method performs the following steps:
   *
   * <ol>
   *   <li>Loads the MCP server configuration from the specified configuration file
   *   <li>Validates if the MCP server is enabled in the configuration
   *   <li>Creates the appropriate server instance based on the configured mode:
   *       <ul>
   *         <li>{@link ServerMode#STDIO} - Standard input/output based server
   *         <li>{@link ServerMode#SSE} - Deprecated HTTP Server-Sent Events based server
   *         <li>{@link ServerMode#STREAMABLE} - Streamable HTTP server
   *       </ul>
   *   <li>Initializes the synchronous MCP server and registers all annotated components
   *   <li>Starts the HTTP server for SSE or STREAMABLE modes
   * </ol>
   *
   * <p>If the MCP server is disabled in the configuration, a warning message will be logged and no
   * server will be started.
   *
   * @param configFileName the name of the configuration file to load
   * @param context the application context for component discovery and localization
   * @see McpApplicationContext
   * @see McpConfigurationLoader
   * @see McpServerConfiguration
   * @see AnnotatedMcpServer
   * @see McpSyncServer
   */
  private static void startMcpServer(String configFileName, McpApplicationContext context) {
    McpConfigurationLoader configurationLoader = new McpConfigurationLoader(configFileName);
    McpServerConfiguration configuration = configurationLoader.loadConfig();
    log.info("Starting MCP server with config: {}", JacksonHelper.toJsonString(configuration));

    if (!configuration.enabled()) {
      log.warn("MCP server is disabled, please check your configuration file.");
      return;
    }

    AnnotatedMcpServer mcpServer = null;
    switch (configuration.mode()) {
      case STDIO -> mcpServer = new McpStdioServer(configuration, context);
      case SSE -> mcpServer = createSseServer(configuration, context);
      case STREAMABLE -> mcpServer = new McpStreamableServer(configuration, context);
    }

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
    mcpServer.awaitShutdown();
  }

  /**
   * Creates an HTTP SSE server instance.
   *
   * @param configuration the server configuration
   * @param context the application context
   * @return a deprecated SSE server instance
   * @deprecated HTTP SSE mode is deprecated; use {@link ServerMode#STREAMABLE} and {@link
   *     McpStreamableServer} instead.
   */
  @Deprecated(since = "0.16.0", forRemoval = true)
  private static AnnotatedMcpServer createSseServer(
      McpServerConfiguration configuration, McpApplicationContext context) {
    return new McpSseServer(configuration, context);
  }
}
