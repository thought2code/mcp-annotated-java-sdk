package com.github.thought2code.mcp.annotated.server;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.configuration.McpServerCapabilities;
import com.github.thought2code.mcp.annotated.configuration.McpServerChangeNotification;
import com.github.thought2code.mcp.annotated.configuration.McpServerConfiguration;
import com.github.thought2code.mcp.annotated.enums.ServerMode;
import com.github.thought2code.mcp.annotated.enums.ServerType;
import com.github.thought2code.mcp.annotated.server.component.completion.CompletionSupport;
import com.github.thought2code.mcp.annotated.server.component.prompt.PromptRegistration;
import com.github.thought2code.mcp.annotated.server.component.resource.ResourceRegistration;
import com.github.thought2code.mcp.annotated.server.component.tool.ToolRegistration;
import com.github.thought2code.mcp.annotated.util.InetHelper;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.servlet.http.HttpServlet;
import java.time.Duration;
import java.util.function.Function;
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
 * @see AnnotatedMcpServer
 * @see McpStdioServer
 * @see McpSseServer
 * @see McpStreamableServer
 * @see McpServerConfiguration
 */
public abstract class McpServerBase implements AnnotatedMcpServer {

  private static final Logger log = LoggerFactory.getLogger(McpServerBase.class);

  /** The server configuration used by this MCP server. */
  protected final McpServerConfiguration configuration;

  /** Application-scoped runtime context used for discovery and localization. */
  protected final McpApplicationContext context;

  /** Underlying Jetty server for HTTP-based modes; {@code null} for STDIO. */
  private JettyHttpServer httpServer;

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
   * <p>This method creates and registers MCP components: resources, prompts, tools, and
   * completions. Each component type is handled by its respective registration class which loads
   * build-time generated definitions and registers them with the server.
   *
   * @param server the synchronous server instance to register components with
   */
  @Override
  public void registerComponents(McpSyncServer server) {
    boolean resourcesRegistered = ResourceRegistration.registerSync(server, context);
    boolean promptsRegistered = PromptRegistration.registerSync(server, context);
    boolean toolsRegistered = ToolRegistration.registerSync(server, context);
    boolean completionsRegistered = !CompletionSupport.allSync(context).isEmpty();
    warnWhenNoComponent(
        resourcesRegistered, promptsRegistered, toolsRegistered, completionsRegistered);
    log.info("MCP sync server components registered successfully");
  }

  /**
   * Registers all MCP server components with the specified asynchronous server.
   *
   * <p>This method creates and registers MCP components: resources, prompts, tools, and
   * completions. Each component type is handled by its respective registration class which loads
   * build-time generated definitions and registers them with the server.
   *
   * @param server the asynchronous server instance to register components with
   */
  @Override
  public void registerComponents(McpAsyncServer server) {
    boolean resourcesRegistered = ResourceRegistration.registerAsync(server, context);
    boolean promptsRegistered = PromptRegistration.registerAsync(server, context);
    boolean toolsRegistered = ToolRegistration.registerAsync(server, context);
    boolean completionsRegistered = !CompletionSupport.allAsync(context).isEmpty();
    warnWhenNoComponent(
        resourcesRegistered, promptsRegistered, toolsRegistered, completionsRegistered);
    log.info("MCP async server components registered successfully");
  }

  /**
   * Creates and returns a fully configured MCP synchronous server instance.
   *
   * <p>This method builds a synchronous server by combining:
   *
   * <ul>
   *   <li>The server capabilities defined by {@link #defineCapabilities()}
   *   <li>All available completion specifications from {@link
   *       CompletionSupport#allSync(McpApplicationContext)}
   *   <li>Server information (name, version) from the configuration
   *   <li>Instructions and request timeout from the configuration
   * </ul>
   *
   * <p>The method uses the synchronization specification provided by the concrete implementation
   * through {@link #createSyncSpecification()}, while lifecycle logging and capability resolution
   * are handled by a shared creation template.
   *
   * @return a fully configured MCP synchronous server ready to start
   * @see McpSyncServer
   * @see CompletionSupport
   */
  @Override
  public McpSyncServer createSyncServer() {
    return createServer(
        ServerType.SYNC,
        capabilities ->
            createSyncSpecification()
                .capabilities(capabilities)
                .completions(CompletionSupport.allSync(context))
                .instructions(configuration.instructions())
                .serverInfo(configuration.name(), configuration.version())
                .requestTimeout(Duration.ofMillis(configuration.requestTimeout()))
                .build());
  }

  /**
   * Creates and returns a fully configured MCP asynchronous server instance.
   *
   * <p>This method builds an asynchronous server by combining:
   *
   * <ul>
   *   <li>The server capabilities defined by {@link #defineCapabilities()}
   *   <li>All available async completion specifications from {@link
   *       CompletionSupport#allAsync(McpApplicationContext)}
   *   <li>Server information (name, version) from the configuration
   *   <li>Instructions and request timeout from the configuration
   * </ul>
   *
   * <p>The method uses the asynchronous specification provided by the concrete implementation
   * through {@link #createAsyncSpecification()}, while lifecycle logging and capability resolution
   * are handled by a shared creation template.
   *
   * @return a fully configured MCP asynchronous server ready to start
   * @see McpAsyncServer
   * @see CompletionSupport
   */
  @Override
  public McpAsyncServer createAsyncServer() {
    return createServer(
        ServerType.ASYNC,
        capabilities ->
            createAsyncSpecification()
                .capabilities(capabilities)
                .completions(CompletionSupport.allAsync(context))
                .instructions(configuration.instructions())
                .serverInfo(configuration.name(), configuration.version())
                .requestTimeout(Duration.ofMillis(configuration.requestTimeout()))
                .build());
  }

  /**
   * Warns when no MCP component definitions are discovered for the current application context.
   *
   * <p>This is a diagnostic signal to help users detect missing annotation-processor output or
   * package-scope misconfiguration.
   *
   * @param resourcesRegistered whether at least one resource component is registered
   * @param promptsRegistered whether at least one prompt component is registered
   * @param toolsRegistered whether at least one tool component is registered
   * @param completionsRegistered whether at least one completion component is registered
   */
  private void warnWhenNoComponent(
      boolean resourcesRegistered,
      boolean promptsRegistered,
      boolean toolsRegistered,
      boolean completionsRegistered) {
    if (resourcesRegistered || promptsRegistered || toolsRegistered || completionsRegistered) {
      return;
    }
    log.warn("No Resource/Prompt/Tool/Completion component models were discovered");
  }

  /**
   * Creates a server with shared lifecycle logging and capability definition.
   *
   * @param type registration type used in logs
   * @param factory server builder that consumes resolved capabilities
   * @return created server instance
   * @param <T> server type
   */
  private <T> T createServer(ServerType type, Function<McpSchema.ServerCapabilities, T> factory) {
    log.info("Creating MCP {} server with name: {}", type.name(), configuration.name());
    McpSchema.ServerCapabilities serverCapabilities = defineCapabilities();
    T server = factory.apply(serverCapabilities);
    log.info("Created MCP {} server successfully with name: {}", type.name(), configuration.name());
    return server;
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
        serverMode.description(),
        InetHelper.findFirstNonLoopbackAddress().getHostAddress(),
        port,
        endpointPath);
    httpServer = new JettyHttpServer().withTransportProvider(transportProvider).bind(port);
    httpServer.start();
  }

  /** Stops the underlying Jetty HTTP server when this MCP server uses an HTTP transport. */
  @Override
  public void stop() {
    if (httpServer != null) {
      httpServer.stop();
      httpServer = null;
    }
  }

  /** Blocks until the underlying Jetty HTTP server stops. */
  @Override
  public void awaitShutdown() {
    if (httpServer != null) {
      httpServer.awaitShutdown();
    }
  }
}
