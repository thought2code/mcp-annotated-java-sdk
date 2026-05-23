package com.github.thought2code.mcp.annotated.configuration;

import com.github.thought2code.mcp.annotated.enums.ServerMode;
import com.github.thought2code.mcp.annotated.exception.McpServerConfigurationException;
import com.github.thought2code.mcp.annotated.util.StringHelper;

/**
 * Utility class for merging MCP server configurations.
 *
 * <p>This class provides static methods to merge base and profile-specific MCP server
 * configurations, where profile values take precedence over base values. The merger supports
 * hierarchical configuration merging for complex nested objects including capabilities, change
 * notifications, and mode-specific settings.
 *
 * @see McpServerConfiguration
 * @author codeboyzhou
 */
public final class McpConfigurationMerger {
  /**
   * Private constructor to prevent instantiation of this utility class.
   *
   * @throws UnsupportedOperationException always thrown when attempting to instantiate
   */
  private McpConfigurationMerger() {
    throw new UnsupportedOperationException("Utility class should not be instantiated");
  }

  /**
   * Merges base and profile MCP server configurations.
   *
   * <p>This method performs a comprehensive merge of two configuration objects, where profile
   * configuration values override base configuration values. The merge process handles all
   * configuration properties including:
   *
   * <ul>
   *   <li>Basic server information (enabled, name, version, type, instructions)
   *   <li>Timeout and capabilities settings
   *   <li>Change notification configuration
   *   <li>Mode-specific settings (SSE or STREAMABLE)
   * </ul>
   *
   * @param base the base configuration containing default values
   * @param profile the profile configuration containing override values
   * @return a new merged {@link McpServerConfiguration} instance
   * @throws McpServerConfigurationException if required configuration properties are missing
   */
  public static McpServerConfiguration merge(
      McpServerConfiguration base, McpServerConfiguration profile) {

    ServerMode mode = mergeValue("mode", base.mode(), profile.mode());

    return new McpServerConfiguration(
        base.profile(),
        mergeValue("enabled", base.enabled(), profile.enabled()),
        mode,
        mergeString("name", base.name(), profile.name()),
        mergeString("version", base.version(), profile.version()),
        mergeValue("type", base.type(), profile.type()),
        mergeString("instructions", base.instructions(), profile.instructions()),
        mergeValue("request-timeout", base.requestTimeout(), profile.requestTimeout()),
        mergeCapabilities(base.capabilities(), profile.capabilities()),
        mergeChangeNotification(base.changeNotification(), profile.changeNotification()),
        mergeSSE(mode, base.sse(), profile.sse()),
        mergeStreamable(mode, base.streamable(), profile.streamable()));
  }

  /**
   * Merges two generic configuration values.
   *
   * <p>This method performs a simple merge where the profile value takes precedence over the base
   * value if the profile value is not null. If the profile value is null, the base value is used.
   *
   * @param <T> the type of the configuration values
   * @param configKey the configuration key name used for validation
   * @param baseValue the base configuration value
   * @param profileValue the profile configuration value
   * @return the merged value (profile value if not null, otherwise base value)
   * @throws McpServerConfigurationException if both values are null
   */
  private static <T> T mergeValue(String configKey, T baseValue, T profileValue) {
    McpConfigurationChecker.checkNull(configKey, baseValue, profileValue);
    return profileValue == null ? baseValue : profileValue;
  }

  /**
   * Merges two string configuration values.
   *
   * <p>This method performs a string merge where the profile value takes precedence over the base
   * value if the profile value is not blank. If the profile value is blank (null, empty, or
   * whitespace-only), the base value is used.
   *
   * @param configKey the configuration key name used for validation
   * @param baseValue the base configuration string value
   * @param profileValue the profile configuration string value
   * @return the merged string value (profile value if not blank, otherwise base value)
   * @throws McpServerConfigurationException if both values are blank
   */
  private static String mergeString(String configKey, String baseValue, String profileValue) {
    McpConfigurationChecker.checkBlank(configKey, baseValue, profileValue);
    return StringHelper.defaultIfBlank(profileValue, baseValue);
  }

  /**
   * Merges server capabilities configurations.
   *
   * <p>This method merges two {@link McpServerCapabilities} objects by combining their individual
   * capability settings. Profile capabilities override base capabilities when present. All
   * capability flags (resource, prompt, tool, completion) must be present in at least one of the
   * configurations.
   *
   * @param base the base capabilities configuration
   * @param profile the profile capabilities configuration
   * @return a new merged {@link McpServerCapabilities} instance
   * @throws McpServerConfigurationException if required capability settings are missing
   */
  private static McpServerCapabilities mergeCapabilities(
      McpServerCapabilities base, McpServerCapabilities profile) {

    McpConfigurationChecker.checkNull("capabilities", base, profile);

    Boolean resource = null;
    Boolean subscribeResource = null;
    Boolean prompt = null;
    Boolean tool = null;
    Boolean completion = null;

    if (base != null) {
      if (base.resource() != null) {
        resource = base.resource();
      }
      if (resource != null && resource && base.subscribeResource() != null) {
        subscribeResource = base.subscribeResource();
      }
      if (base.prompt() != null) {
        prompt = base.prompt();
      }
      if (base.tool() != null) {
        tool = base.tool();
      }
      if (base.completion() != null) {
        completion = base.completion();
      }
    }

    if (profile != null) {
      if (profile.resource() != null) {
        resource = profile.resource();
      }
      if (resource != null && resource && profile.subscribeResource() != null) {
        subscribeResource = profile.subscribeResource();
      }
      if (profile.prompt() != null) {
        prompt = profile.prompt();
      }
      if (profile.tool() != null) {
        tool = profile.tool();
      }
      if (profile.completion() != null) {
        completion = profile.completion();
      }
    }

    McpConfigurationChecker.checkNull("capabilities.resource", resource);
    McpConfigurationChecker.checkNull("capabilities.prompt", prompt);
    McpConfigurationChecker.checkNull("capabilities.tool", tool);
    McpConfigurationChecker.checkNull("capabilities.completion", completion);

    return new McpServerCapabilities(resource, subscribeResource, prompt, tool, completion);
  }

  /**
   * Merges server change notification configurations.
   *
   * <p>This method merges two {@link McpServerChangeNotification} objects by combining their
   * individual notification settings. Profile notification settings override base settings when
   * present. All notification flags (resource, prompt, tool) must be present in at least one of the
   * configurations.
   *
   * @param base the base change notification configuration
   * @param profile the profile change notification configuration
   * @return a new merged {@link McpServerChangeNotification} instance
   * @throws McpServerConfigurationException if required notification settings are missing
   */
  private static McpServerChangeNotification mergeChangeNotification(
      McpServerChangeNotification base, McpServerChangeNotification profile) {

    McpConfigurationChecker.checkNull("change-notification", base, profile);

    Boolean resource = null;
    Boolean prompt = null;
    Boolean tool = null;

    if (base != null) {
      if (base.resource() != null) {
        resource = base.resource();
      }
      if (base.prompt() != null) {
        prompt = base.prompt();
      }
      if (base.tool() != null) {
        tool = base.tool();
      }
    }

    if (profile != null) {
      if (profile.resource() != null) {
        resource = profile.resource();
      }
      if (profile.prompt() != null) {
        prompt = profile.prompt();
      }
      if (profile.tool() != null) {
        tool = profile.tool();
      }
    }

    McpConfigurationChecker.checkNull("change-notification.resource", resource);
    McpConfigurationChecker.checkNull("change-notification.prompt", prompt);
    McpConfigurationChecker.checkNull("change-notification.tool", tool);

    return new McpServerChangeNotification(resource, prompt, tool);
  }

  /**
   * Merges SSE (Server-Sent Events) configurations.
   *
   * <p>This method merges two {@link McpServerSSE} objects by combining their SSE-specific
   * settings. Profile settings override base settings when present. This method only processes SSE
   * configurations when the server mode is SSE. All required SSE settings (messageEndpoint,
   * endpoint, baseUrl, port) must be present.
   *
   * @param mode the server mode to determine if SSE merging should be performed
   * @param base the base SSE configuration
   * @param profile the profile SSE configuration
   * @return a new merged {@link McpServerSSE} instance, or null if mode is not SSE
   * @throws McpServerConfigurationException if required SSE settings are missing
   * @deprecated HTTP SSE mode is deprecated; use {@link ServerMode#STREAMABLE} instead.
   */
  @Deprecated(since = "0.16.0", forRemoval = true)
  private static McpServerSSE mergeSSE(ServerMode mode, McpServerSSE base, McpServerSSE profile) {
    if (mode != ServerMode.SSE) {
      return null;
    }

    McpConfigurationChecker.checkNull("sse", base, profile);

    String messageEndpoint = null;
    String endpoint = null;
    String baseUrl = null;
    Integer port = null;

    if (base != null) {
      if (base.messageEndpoint() != null) {
        messageEndpoint = base.messageEndpoint();
      }
      if (base.endpoint() != null) {
        endpoint = base.endpoint();
      }
      if (base.baseUrl() != null) {
        baseUrl = base.baseUrl();
      }
      if (base.port() != null) {
        port = base.port();
      }
    }

    if (profile != null) {
      if (profile.messageEndpoint() != null) {
        messageEndpoint = profile.messageEndpoint();
      }
      if (profile.endpoint() != null) {
        endpoint = profile.endpoint();
      }
      if (profile.baseUrl() != null) {
        baseUrl = profile.baseUrl();
      }
      if (profile.port() != null) {
        port = profile.port();
      }
    }

    McpConfigurationChecker.checkBlank("sse.message-endpoint", messageEndpoint);
    McpConfigurationChecker.checkBlank("sse.endpoint", endpoint);
    McpConfigurationChecker.checkBlank("sse.base-url", baseUrl);
    McpConfigurationChecker.checkNull("sse.port", port);

    return new McpServerSSE(messageEndpoint, endpoint, baseUrl, port);
  }

  /**
   * Merges streamable server configurations.
   *
   * <p>This method merges two {@link McpServerStreamable} objects by combining their
   * streamable-specific settings. Profile settings override base settings when present. This method
   * only processes streamable configurations when the server mode is STREAMABLE. All required
   * streamable settings (mcpEndpoint, disallowDelete, keepAliveInterval, port) must be present.
   *
   * @param mode the server mode to determine if streamable merging should be performed
   * @param base the base streamable configuration
   * @param profile the profile streamable configuration
   * @return a new merged {@link McpServerStreamable} instance, or null if mode is not STREAMABLE
   * @throws McpServerConfigurationException if required streamable settings are missing
   */
  private static McpServerStreamable mergeStreamable(
      ServerMode mode, McpServerStreamable base, McpServerStreamable profile) {

    if (mode != ServerMode.STREAMABLE) {
      return null;
    }

    McpConfigurationChecker.checkNull("streamable", base, profile);

    String mcpEndpoint = null;
    Boolean disallowDelete = null;
    Long keepAliveInterval = null;
    Integer port = null;

    if (base != null) {
      if (base.mcpEndpoint() != null) {
        mcpEndpoint = base.mcpEndpoint();
      }
      if (base.disallowDelete() != null) {
        disallowDelete = base.disallowDelete();
      }
      if (base.keepAliveInterval() != null) {
        keepAliveInterval = base.keepAliveInterval();
      }
      if (base.port() != null) {
        port = base.port();
      }
    }

    if (profile != null) {
      if (profile.mcpEndpoint() != null) {
        mcpEndpoint = profile.mcpEndpoint();
      }
      if (profile.disallowDelete() != null) {
        disallowDelete = profile.disallowDelete();
      }
      if (profile.keepAliveInterval() != null) {
        keepAliveInterval = profile.keepAliveInterval();
      }
      if (profile.port() != null) {
        port = profile.port();
      }
    }

    McpConfigurationChecker.checkBlank("streamable.mcp-endpoint", mcpEndpoint);
    McpConfigurationChecker.checkNull("streamable.disallow-delete", disallowDelete);
    McpConfigurationChecker.checkNull("streamable.keep-alive-interval", keepAliveInterval);
    McpConfigurationChecker.checkNull("streamable.port", port);

    return new McpServerStreamable(mcpEndpoint, disallowDelete, keepAliveInterval, port);
  }
}
