package com.github.thought2code.mcp.annotated.configuration;

import com.github.thought2code.mcp.annotated.enums.ServerMode;
import com.github.thought2code.mcp.annotated.exception.McpServerConfigurationException;
import com.github.thought2code.mcp.annotated.util.StringHelper;

/**
 * Utility class for validating MCP server configuration properties.
 *
 * <p>This class provides static methods to perform comprehensive validation of MCP server
 * configuration objects, ensuring that all required fields are present and properly configured. It
 * validates both base configuration properties and mode-specific settings for SSE and STREAMABLE
 * server modes.
 *
 * @see McpServerConfiguration
 * @author codeboyzhou
 */
public final class McpConfigurationChecker {
  /**
   * Private constructor to prevent instantiation of this utility class.
   *
   * @throws UnsupportedOperationException always thrown when attempting to instantiate
   */
  private McpConfigurationChecker() {
    throw new UnsupportedOperationException("Utility class should not be instantiated");
  }

  /**
   * Performs comprehensive validation of the MCP server configuration.
   *
   * <p>This method validates all required configuration properties including:
   *
   * <ul>
   *   <li>Basic server information (enabled, mode, name, version, type, instructions)
   *   <li>Timeout and capabilities settings
   *   <li>Change notification configuration
   *   <li>Mode-specific settings (SSE or STREAMABLE)
   * </ul>
   *
   * @param configuration the MCP server configuration to validate
   * @throws McpServerConfigurationException if any required configuration property is missing
   */
  public static void check(McpServerConfiguration configuration) {
    checkNull("enabled", configuration.enabled());
    checkNull("mode", configuration.mode());
    checkBlank("name", configuration.name());
    checkBlank("version", configuration.version());
    checkNull("type", configuration.type());
    checkBlank("instructions", configuration.instructions());
    checkNull("request-timeout", configuration.requestTimeout());
    checkNull("capabilities", configuration.capabilities());
    checkNull("resource", configuration.capabilities().resource());
    if (configuration.capabilities().resource()) {
      checkNull("subscribe-resource", configuration.capabilities().subscribeResource());
    }
    checkNull("prompt", configuration.capabilities().prompt());
    checkNull("tool", configuration.capabilities().tool());
    checkNull("completion", configuration.capabilities().completion());
    checkNull("change-notification", configuration.changeNotification());
    checkNull("resource", configuration.changeNotification().resource());
    checkNull("prompt", configuration.changeNotification().prompt());
    checkNull("tool", configuration.changeNotification().tool());
    if (configuration.mode() == ServerMode.SSE) {
      checkSse(configuration);
    }
    if (configuration.mode() == ServerMode.STREAMABLE) {
      checkNull("streamable", configuration.streamable());
      checkBlank("mcp-endpoint", configuration.streamable().mcpEndpoint());
      checkNull("disallow-delete", configuration.streamable().disallowDelete());
      checkNull("keep-alive-interval", configuration.streamable().keepAliveInterval());
      checkNull("port", configuration.streamable().port());
    }
  }

  /**
   * Validates SSE-specific configuration properties.
   *
   * @param configuration the MCP server configuration to validate
   * @deprecated HTTP SSE mode is deprecated; use {@link ServerMode#STREAMABLE} instead.
   */
  @Deprecated(since = "0.16.0", forRemoval = true)
  private static void checkSse(McpServerConfiguration configuration) {
    checkNull("sse", configuration.sse());
    checkBlank("message-endpoint", configuration.sse().messageEndpoint());
    checkBlank("endpoint", configuration.sse().endpoint());
    checkBlank("base-url", configuration.sse().baseUrl());
    checkNull("port", configuration.sse().port());
  }

  /**
   * Validates that at least one of two configuration values is not null.
   *
   * <p>This method is typically used to validate profile-based configurations where a base value
   * and a profile-specific value can both provide the same configuration property. At least one of
   * them must be non-null.
   *
   * @param <T> the type of the configuration values
   * @param configKey the configuration key name used for error reporting
   * @param baseValue the base configuration value
   * @param profileValue the profile-specific configuration value
   * @throws McpServerConfigurationException if both values are null
   */
  public static <T> void checkNull(String configKey, T baseValue, T profileValue) {
    if (baseValue == null && profileValue == null) {
      throw new McpServerConfigurationException(
          String.format("Missing config key '%s' in the configuration file.", configKey));
    }
  }

  /**
   * Validates that a single configuration value is not null.
   *
   * <p>This method performs a simple null check on a configuration value and throws an exception if
   * the value is null, indicating that a required configuration property is missing.
   *
   * @param <T> the type of the configuration value
   * @param configKey the configuration key name used for error reporting
   * @param value the configuration value to validate
   * @throws McpServerConfigurationException if the value is null
   */
  public static <T> void checkNull(String configKey, T value) {
    if (value == null) {
      throw new McpServerConfigurationException(
          String.format("Missing config key '%s' in the configuration file.", configKey));
    }
  }

  /**
   * Validates that at least one of two string configuration values is not blank.
   *
   * <p>This method is typically used to validate profile-based configurations where a base value
   * and a profile-specific value can both provide the same string configuration property. At least
   * one of them must be non-blank. A value is considered blank if it is null, empty, or contains
   * only whitespace.
   *
   * @param configKey the configuration key name used for error reporting
   * @param baseValue the base configuration string value
   * @param profileValue the profile-specific configuration string value
   * @throws McpServerConfigurationException if both values are blank
   */
  public static void checkBlank(String configKey, String baseValue, String profileValue) {
    if (StringHelper.isBlank(baseValue) && StringHelper.isBlank(profileValue)) {
      throw new McpServerConfigurationException(
          String.format("Missing config key '%s' in the configuration file.", configKey));
    }
  }

  /**
   * Validates that a single string configuration value is not blank.
   *
   * <p>This method performs a blank check on a string configuration value and throws an exception
   * if the value is blank. A value is considered blank if it is null, empty, or contains only
   * whitespace.
   *
   * @param configKey the configuration key name used for error reporting
   * @param value the configuration string value to validate
   * @throws McpServerConfigurationException if the value is blank
   */
  public static void checkBlank(String configKey, String value) {
    if (StringHelper.isBlank(value)) {
      throw new McpServerConfigurationException(
          String.format("Missing config key '%s' in the configuration file.", configKey));
    }
  }
}
