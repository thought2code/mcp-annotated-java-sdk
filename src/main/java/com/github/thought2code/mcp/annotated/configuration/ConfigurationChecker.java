package com.github.thought2code.mcp.annotated.configuration;

import com.github.thought2code.mcp.annotated.exception.McpServerConfigurationException;
import com.github.thought2code.mcp.annotated.util.StringHelper;

/**
 * Utility class for validating MCP server configuration properties.
 *
 * <p>This class provides static methods to perform comprehensive validation of MCP server
 * configuration objects, ensuring that all required fields are present and properly configured. It
 * validates both base configuration properties and mode-specific settings for STREAMABLE server
 * mode.
 *
 * @see ServerConfiguration
 * @author codeboyzhou
 */
public final class ConfigurationChecker {

  private ConfigurationChecker() {
    throw new UnsupportedOperationException("Utility class should not be instantiated");
  }

  /**
   * Performs comprehensive validation of the MCP server configuration.
   *
   * @param configuration the MCP server configuration to validate
   * @throws McpServerConfigurationException if any required configuration property is missing
   */
  public static void check(ServerConfiguration configuration) {
    checkCore(configuration);
    checkCapabilities(configuration.capabilities());
    checkChangeNotification(configuration.changeNotification());
    switch (configuration.mode()) {
      case STREAMABLE -> checkStreamable(configuration.streamable());
      case STDIO -> {}
    }
  }

  private static void checkCore(ServerConfiguration configuration) {
    checkNull("enabled", configuration.enabled());
    checkNull("mode", configuration.mode());
    checkBlank("name", configuration.name());
    checkBlank("version", configuration.version());
    checkNull("type", configuration.type());
    checkBlank("instructions", configuration.instructions());
    checkNull("request-timeout", configuration.requestTimeout());
    checkNull("capabilities", configuration.capabilities());
    checkNull("change-notification", configuration.changeNotification());
  }

  private static void checkCapabilities(ServerCapabilities capabilities) {
    checkNull("capabilities.resource", capabilities.resource());
    if (capabilities.resource()) {
      checkNull("capabilities.subscribe-resource", capabilities.subscribeResource());
    }
    checkNull("capabilities.prompt", capabilities.prompt());
    checkNull("capabilities.tool", capabilities.tool());
    checkNull("capabilities.completion", capabilities.completion());
  }

  private static void checkChangeNotification(ServerChangeNotification changeNotification) {
    checkNull("change-notification.resource", changeNotification.resource());
    checkNull("change-notification.prompt", changeNotification.prompt());
    checkNull("change-notification.tool", changeNotification.tool());
  }

  private static void checkStreamable(ServerStreamable streamable) {
    checkNull("streamable", streamable);
    checkBlank("streamable.mcp-endpoint", streamable.mcpEndpoint());
    checkNull("streamable.disallow-delete", streamable.disallowDelete());
    checkNull("streamable.keep-alive-interval", streamable.keepAliveInterval());
    checkNull("streamable.port", streamable.port());
  }

  private static <T> void checkNull(String configKey, T value) {
    if (value == null) {
      throw new McpServerConfigurationException(
          String.format("Missing config key '%s' in the configuration file.", configKey));
    }
  }

  private static void checkBlank(String configKey, String value) {
    if (StringHelper.isBlank(value)) {
      throw new McpServerConfigurationException(
          String.format("Missing config key '%s' in the configuration file.", configKey));
    }
  }
}
