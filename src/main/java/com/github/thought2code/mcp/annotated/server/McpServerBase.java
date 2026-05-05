package com.github.thought2code.mcp.annotated.server;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.configuration.McpServerCapabilities;
import com.github.thought2code.mcp.annotated.configuration.McpServerChangeNotification;
import com.github.thought2code.mcp.annotated.configuration.McpServerConfiguration;
import com.github.thought2code.mcp.annotated.enums.ServerMode;
import com.github.thought2code.mcp.annotated.server.component.McpComponentRegistrar;
import com.github.thought2code.mcp.annotated.server.component.McpServerCompletion;
import com.github.thought2code.mcp.annotated.util.InetHelper;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.servlet.http.HttpServlet;
import java.time.Duration;
import java.util.ServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class that provides a common implementation for MCP (Model Context Protocol)
 * servers.
 *
 * <p>This class implements the core functionality shared across different MCP server
 * implementations, including capability definition, component registration, and server creation. It
 * serves as a foundation for concrete server implementations that handle different transport
 * mechanisms (STDIO, SSE, STREAMABLE).
 *
 * <p>The class manages server configuration and provides default implementations for:
 *
 * <ul>
 *   <li>Defining server capabilities based on configuration
 *   <li>Registering MCP components (resources, prompts, tools) with both sync and async servers
 *   <li>Creating a configured synchronous or asynchronous server instance
 * </ul>
 *
 * <p>Concrete implementations need only provide the specific synchronization/asynchronous
 * specification for their transport mechanism by implementing the {@link
 * #createSyncSpecification()} and {@link #createAsyncSpecification()} methods.
 *
 * @author codeboyzhou
 * @see McpServer
 * @see McpStdioServer
 * @see McpSseServer
 * @see McpStreamableServer
 * @see McpServerConfiguration
 */
public abstract class McpServerBase implements McpServer {

  private static final Logger log = LoggerFactory.getLogger(McpServerBase.class);

  /** The server configuration used by this MCP server. */
  protected final McpServerConfiguration configuration;

  /** Application-scoped runtime context used for discovery and localization. */
  protected final McpApplicationContext context;

  /**
   * Constructs a new {@link McpServerBase} with the specified configuration and application
   * context.
   *
   * @param configuration the server configuration
   * @param context the application-scoped runtime context for component discovery and localization
   */
  public McpServerBase(McpServerConfiguration configuration, McpApplicationContext context) {
    this.configuration = configuration;
    this.context = context;
  }

  /**
   * Defines and returns the server capabilities based on the configuration.
   *
   * <p>This method reads the capability settings from the server configuration and constructs a
   * {@link McpSchema.ServerCapabilities} object that specifies which features are enabled
   * (resources, prompts, tools, completions) and their respective change notification settings.
   *
   * @return a configured ServerCapabilities object reflecting the server's supported features
   * @see McpServerCapabilities
   * @see McpServerChangeNotification
   */
  @Override
  public McpSchema.ServerCapabilities defineCapabilities() {
    McpServerCapabilities capabilitiesConfig = configuration.capabilities();
    McpServerChangeNotification serverChangeNotification = configuration.changeNotification();

    McpSchema.ServerCapabilities.Builder capabilities = McpSchema.ServerCapabilities.builder();

    if (capabilitiesConfig.resource()) {
      final Boolean subscribe = capabilitiesConfig.subscribeResource();
      final Boolean changeNotification = serverChangeNotification.resource();
      capabilities.resources(subscribe, changeNotification);
    }

    if (capabilitiesConfig.prompt()) {
      capabilities.prompts(serverChangeNotification.prompt());
    }

    if (capabilitiesConfig.tool()) {
      capabilities.tools(serverChangeNotification.tool());
    }

    if (capabilitiesConfig.completion()) {
      capabilities.completions();
    }

    return capabilities.build();
  }

  /**
   * Registers all MCP server components with the specified synchronous server.
   *
   * <p>This method creates and registers the three main types of MCP components: resources,
   * prompts, and tools. Each component type is handled by its respective registration class which
   * scans for annotated methods and registers them with the server.
   *
   * @param server the synchronous server instance to register components with
   */
  @Override
  public void registerComponents(McpSyncServer server) {
    log.info("Registering MCP server components (sync)");
    ServiceLoader<McpComponentRegistrar> loader = ServiceLoader.load(McpComponentRegistrar.class);
    for (McpComponentRegistrar registrar : loader) {
      registrar.register(server, context);
    }
    log.info("MCP server components registered successfully (sync)");
  }

  /**
   * Registers all MCP server components with the specified asynchronous server.
   *
   * <p>This method creates and registers the three main types of MCP components: resources,
   * prompts, and tools. Each component type is handled by its respective registration class which
   * scans for annotated methods and registers them with the server. Registration is performed
   * asynchronously and each registration is subscribed to immediately.
   *
   * @param server the asynchronous server instance to register components with
   */
  @Override
  public void registerComponents(McpAsyncServer server) {
    log.info("Registering MCP server components (async)");
    ServiceLoader<McpComponentRegistrar> loader = ServiceLoader.load(McpComponentRegistrar.class);
    for (McpComponentRegistrar registrar : loader) {
      registrar.register(server, context);
    }
    log.info("MCP server components registered successfully (async)");
  }

  /**
   * Creates and returns a fully configured MCP synchronous server instance.
   *
   * <p>This method builds a synchronous server by combining:
   *
   * <ul>
   *   <li>The server capabilities defined by {@link #defineCapabilities()}
   *   <li>All available completion specifications from {@link
   *       McpServerCompletion#allSync(McpApplicationContext)}
   *   <li>Server information (name, version) from the configuration
   *   <li>Instructions and request timeout from the configuration
   * </ul>
   *
   * <p>The method uses the synchronization specification provided by the concrete implementation
   * through {@link #createSyncSpecification()}.
   *
   * @return a fully configured MCP synchronous server ready to start
   * @see McpSyncServer
   * @see McpServerCompletion
   */
  @Override
  public McpSyncServer createSyncServer() {
    log.info("Creating McpSyncServer with name: {}", configuration.name());
    McpSchema.ServerCapabilities serverCapabilities = defineCapabilities();
    McpSyncServer mcpSyncServer =
        createSyncSpecification()
            .capabilities(serverCapabilities)
            .completions(McpServerCompletion.allSync(context))
            .instructions(configuration.instructions())
            .serverInfo(configuration.name(), configuration.version())
            .requestTimeout(Duration.ofMillis(configuration.requestTimeout()))
            .build();
    log.info("Created McpSyncServer successfully with name: {}", configuration.name());
    return mcpSyncServer;
  }

  /**
   * Creates and returns a fully configured MCP asynchronous server instance.
   *
   * <p>This method builds an asynchronous server by combining:
   *
   * <ul>
   *   <li>The server capabilities defined by {@link #defineCapabilities()}
   *   <li>All available async completion specifications from {@link
   *       McpServerCompletion#allAsync(McpApplicationContext)}
   *   <li>Server information (name, version) from the configuration
   *   <li>Instructions and request timeout from the configuration
   * </ul>
   *
   * <p>The method uses the asynchronous specification provided by the concrete implementation
   * through {@link #createAsyncSpecification()}.
   *
   * @return a fully configured MCP asynchronous server ready to start
   * @see McpAsyncServer
   * @see McpServerCompletion
   */
  @Override
  public McpAsyncServer createAsyncServer() {
    log.info("Creating McpAsyncServer with name: {}", configuration.name());
    McpSchema.ServerCapabilities serverCapabilities = defineCapabilities();
    McpAsyncServer mcpAsyncServer =
        createAsyncSpecification()
            .capabilities(serverCapabilities)
            .completions(McpServerCompletion.allAsync(context))
            .instructions(configuration.instructions())
            .serverInfo(configuration.name(), configuration.version())
            .requestTimeout(Duration.ofMillis(configuration.requestTimeout()))
            .build();
    log.info("Created McpAsyncServer successfully with name: {}", configuration.name());
    return mcpAsyncServer;
  }

  /**
   * Starts a Jetty HTTP server with a transport provider.
   *
   * <p>This helper centralizes the shared HTTP startup lifecycle for SSE and Streamable modes so
   * concrete servers only provide transport-specific configuration.
   *
   * @param serverMode server mode used in startup logs
   * @param endpointPath endpoint path used in startup logs
   * @param transportProvider transport servlet to register
   * @param port server port
   */
  protected final void startHttpServer(
      ServerMode serverMode, String endpointPath, HttpServlet transportProvider, int port) {
    log.info(
        "Starting Jetty-based MCP {} server on http://{}:{}{}",
        serverMode.name(),
        InetHelper.findFirstNonLoopbackAddress().getHostAddress(),
        port,
        endpointPath);
    new JettyHttpServer().withTransportProvider(transportProvider).bind(port).start();
  }
}
