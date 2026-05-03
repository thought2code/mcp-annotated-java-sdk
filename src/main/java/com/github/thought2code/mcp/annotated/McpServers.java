package com.github.thought2code.mcp.annotated;

import com.github.thought2code.mcp.annotated.configuration.McpConfigurationLoader;
import com.github.thought2code.mcp.annotated.configuration.McpServerConfiguration;
import com.github.thought2code.mcp.annotated.enums.ServerMode;
import com.github.thought2code.mcp.annotated.exception.McpServerException;
import com.github.thought2code.mcp.annotated.reflect.ReflectionsProvider;
import com.github.thought2code.mcp.annotated.server.McpServer;
import com.github.thought2code.mcp.annotated.server.McpSseServer;
import com.github.thought2code.mcp.annotated.server.McpStdioServer;
import com.github.thought2code.mcp.annotated.server.McpStreamableServer;
import com.github.thought2code.mcp.annotated.server.component.ResourceBundleProvider;
import com.github.thought2code.mcp.annotated.util.JacksonHelper;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.util.Assert;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton class that provides methods to start and manage MCP (Model Context Protocol) servers.
 *
 * <p>This class serves as the main entry point for starting MCP servers in different modes. It
 * follows the singleton design pattern and provides convenient methods for starting servers in
 * STDIO, SSE, or STREAMABLE modes, either programmatically or through configuration files.
 *
 * <p>The class supports three server modes:
 *
 * <ul>
 *   <li>STDIO - Standard input/output communication (default for CLI tools)
 *   <li>SSE - Server-Sent Events for HTTP-based real-time communication
 *   <li>STREAMABLE - HTTP streaming for web applications
 * </ul>
 *
 * <p>Before starting a server, the class must be initialized by calling {@link #run(Class,
 * String[])} with the main application class. This initializes reflection scanning and resource
 * bundles for i18n support.
 *
 * @author codeboyzhou
 * @see McpServer
 * @see McpStdioServer
 * @see McpSseServer
 * @see McpStreamableServer
 * @see McpServerConfiguration
 * @deprecated This class is deprecated and will be removed in a future version.
 *     <p>Please use {@link McpApplication} instead.
 */
@Deprecated(since = "0.13.0", forRemoval = true)
public final class McpServers {

  private static final Logger log = LoggerFactory.getLogger(McpServers.class);

  /** Application-scoped runtime context. */
  private final McpApplicationContext context;

  /** Private constructor to prevent instantiation of this singleton class. */
  private McpServers(McpApplicationContext context) {
    this.context = context;
  }

  /**
   * Initializes the McpServers singleton with the specified main class.
   *
   * <p>This method must be called before starting any MCP server. It performs necessary
   * initialization tasks including:
   *
   * <ul>
   *   <li>Initializing reflection scanning for annotated components
   *   <li>Loading resource bundles for internationalization support
   * </ul>
   *
   * <p>The main class is used to determine the base package for reflection scanning and to check
   * for the {@link com.github.thought2code.mcp.annotated.annotation.McpI18nEnabled} annotation for
   * i18n configuration.
   *
   * @param mainClass the main application class used for initialization
   * @param args command line arguments (currently not used but reserved for future use)
   * @return the singleton instance of McpServers
   * @throws NullPointerException if mainClass is null
   * @see ReflectionsProvider#initializeReflectionsInstance(Class)
   * @see ResourceBundleProvider#loadResourceBundle(Class)
   */
  @Deprecated(since = "0.13.0", forRemoval = true)
  public static McpServers run(Class<?> mainClass, String[] args) {
    log.info("Initializing {} with args: {}", mainClass.getSimpleName(), args);
    McpApplicationContext context = McpApplicationContext.from(mainClass);
    McpServers servers = new McpServers(context);
    log.info("{} initialized successfully", mainClass.getSimpleName());
    return servers;
  }

  /**
   * Starts an MCP server in STDIO mode with the specified configuration.
   *
   * <p>This method configures and starts a server that communicates via standard input/output. The
   * server is enabled and the mode is set to STDIO before starting.
   *
   * <p>STDIO mode is typically used for command-line tools and applications that communicate
   * through standard streams.
   *
   * @param configuration the builder containing server configuration settings
   * @throws NullPointerException if configuration is null
   * @see ServerMode#STDIO
   * @see McpStdioServer
   * @see McpServerConfiguration.Builder
   */
  @Deprecated(since = "0.13.0", forRemoval = true)
  public void startStdioServer(McpServerConfiguration.Builder configuration) {
    doStartServer(configuration.mode(ServerMode.STDIO).build());
  }

  /**
   * Starts an MCP server in SSE mode with the specified configuration.
   *
   * <p>This method configures and starts a server that uses Server-Sent Events for HTTP-based
   * real-time communication. The server is enabled and the mode is set to SSE before starting.
   *
   * <p>SSE mode is suitable for web applications requiring real-time updates and bidirectional
   * communication.
   *
   * @param configuration the builder containing server configuration settings
   * @throws NullPointerException if configuration is null
   * @see ServerMode#SSE
   * @see McpSseServer
   * @see McpServerConfiguration.Builder
   */
  @Deprecated(since = "0.13.0", forRemoval = true)
  public void startSseServer(McpServerConfiguration.Builder configuration) {
    doStartServer(configuration.mode(ServerMode.SSE).build());
  }

  /**
   * Starts an MCP server in STREAMABLE mode with the specified configuration.
   *
   * <p>This method configures and starts a server that uses HTTP streaming for communication. The
   * server is enabled and the mode is set to STREAMABLE before starting.
   *
   * <p>STREAMABLE mode is designed for web applications that require HTTP-based streaming
   * communication.
   *
   * @param configuration the builder containing server configuration settings
   * @throws NullPointerException if configuration is null
   * @see ServerMode#STREAMABLE
   * @see McpStreamableServer
   * @see McpServerConfiguration.Builder
   */
  @Deprecated(since = "0.13.0", forRemoval = true)
  public void startStreamableServer(McpServerConfiguration.Builder configuration) {
    doStartServer(configuration.mode(ServerMode.STREAMABLE).build());
  }

  /**
   * Starts an MCP server using configuration from the specified file.
   *
   * <p>This method loads server configuration from a JSON or YAML file and starts the server
   * according to the loaded configuration. The configuration file must contain valid server
   * settings including mode, port, and other parameters.
   *
   * @param configFileName the path to the configuration file
   * @throws IllegalArgumentException if configFileName is null
   * @throws McpServerException if the configuration file cannot be loaded or is invalid
   * @see McpConfigurationLoader
   * @see McpServerConfiguration
   */
  @Deprecated(since = "0.13.0", forRemoval = true)
  public void startServer(String configFileName) {
    Assert.notNull(configFileName, "configFileName must not be null");
    log.info("Starting MCP server with configuration file: {}", configFileName);
    McpConfigurationLoader configLoader = new McpConfigurationLoader(configFileName);
    doStartServer(configLoader.loadConfig());
  }

  /**
   * Starts an MCP server using default configuration.
   *
   * <p>This method loads server configuration from the default location (typically a configuration
   * file in the classpath or working directory) and starts the server according to the loaded
   * configuration.
   *
   * @throws McpServerException if the default configuration cannot be loaded or is invalid
   * @see McpConfigurationLoader
   * @see McpServerConfiguration
   */
  @Deprecated(since = "0.13.0", forRemoval = true)
  public void startServer() {
    log.info("Starting MCP server with default configuration");
  }

  /**
   * Starts an MCP server based on the provided configuration.
   *
   * <p>This private method creates and configures an MCP server instance based on the server mode
   * specified in the configuration. It performs the following steps:
   *
   * <ol>
   *   <li>Checks if the server is enabled in the configuration
   *   <li>Creates the appropriate server instance based on mode (STDIO, SSE, or STREAMABLE)
   *   <li>Creates the sync server instance
   *   <li>Registers all components (tools, resources, prompts) with the server
   *   <li>Starts the HTTP server if the mode is SSE or STREAMABLE
   * </ol>
   *
   * <p>If the server is disabled in the configuration, this method logs a warning message and
   * returns without starting any server.
   *
   * @param configuration the server configuration containing mode and other settings
   * @see McpServerConfiguration
   * @see McpServer
   * @see McpSyncServer
   * @see ServerMode
   */
  private void doStartServer(McpServerConfiguration configuration) {
    log.info("Starting MCP server with config: {}", JacksonHelper.toJsonString(configuration));
    if (configuration.enabled()) {
      McpServer mcpServer = null;
      switch (configuration.mode()) {
        case STDIO -> mcpServer = new McpStdioServer(configuration, context);
        case SSE -> mcpServer = new McpSseServer(configuration, context);
        case STREAMABLE -> mcpServer = new McpStreamableServer(configuration, context);
      }

      Objects.requireNonNull(mcpServer, "mcpServer must not be null");
      McpSyncServer mcpSyncServer = mcpServer.createSyncServer();
      mcpServer.registerComponents(mcpSyncServer);
      mcpServer.start();
    } else {
      log.warn("MCP server is disabled, please check your configuration file.");
    }
  }
}
