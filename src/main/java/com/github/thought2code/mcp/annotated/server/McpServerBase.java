package com.github.thought2code.mcp.annotated.server;

import com.github.thought2code.mcp.annotated.configuration.McpServerCapabilities;
import com.github.thought2code.mcp.annotated.configuration.McpServerChangeNotification;
import com.github.thought2code.mcp.annotated.configuration.McpServerConfiguration;
import com.github.thought2code.mcp.annotated.server.component.*;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import java.time.Duration;
import java.util.ServiceLoader;
import org.jetbrains.annotations.NotNull;
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
 *   <li>Registering MCP components (resources, prompts, tools)
 *   <li>Creating a configured synchronous server instance
 * </ul>
 *
 * <p>Concrete implementations need only provide the specific synchronization specification for
 * their transport mechanism by implementing the {@link #createSyncSpecification()} method.
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

  /**
   * Constructs a new {@link McpServerBase} with the specified configuration.
   *
   * @param configuration the server configuration containing all settings for the MCP server,
   *     including capabilities, timeouts, and transport settings
   */
  public McpServerBase(@NotNull McpServerConfiguration configuration) {
    this.configuration = configuration;
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
   * @see McpServerResource
   * @see McpServerPrompt
   * @see McpServerTool
   */
  @Override
  public void registerComponents(McpSyncServer server) {
    log.info("Registering MCP server components");
    ServiceLoader<McpComponentRegistrar> loader = ServiceLoader.load(McpComponentRegistrar.class);
    for (McpComponentRegistrar registrar : loader) {
      registrar.register(server);
    }
    log.info("MCP server components registered successfully");
  }

  /**
   * Creates and returns a fully configured MCP synchronous server instance.
   *
   * <p>This method builds a synchronous server by combining:
   *
   * <ul>
   *   <li>The server capabilities defined by {@link #defineCapabilities()}
   *   <li>All available completion specifications from {@link McpServerCompletion#all()}
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
            .completions(McpServerCompletion.all())
            .instructions(configuration.instructions())
            .serverInfo(configuration.name(), configuration.version())
            .requestTimeout(Duration.ofMillis(configuration.requestTimeout()))
            .build();
    log.info("Created McpSyncServer successfully with name: {}", configuration.name());
    return mcpSyncServer;
  }
}
