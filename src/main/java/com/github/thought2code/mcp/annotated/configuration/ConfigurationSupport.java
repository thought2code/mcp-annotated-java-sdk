package com.github.thought2code.mcp.annotated.configuration;

import com.github.thought2code.mcp.annotated.enums.ServerMode;
import com.github.thought2code.mcp.annotated.util.StringHelper;

/**
 * Post-processing helpers for MCP server configuration loaded from YAML.
 *
 * <p>Jackson profile merging handles field overlays; this class applies MCP-specific rules that are
 * not expressible through generic YAML merge alone.
 */
public final class ConfigurationSupport {

  private ConfigurationSupport() {
    throw new UnsupportedOperationException("Utility class should not be instantiated");
  }

  /**
   * Applies MCP-specific rules after base and profile YAML have been merged.
   *
   * <ul>
   *   <li>Preserves the profile name declared in the base configuration file
   *   <li>Clears transport settings that do not apply to the resolved server mode
   * </ul>
   *
   * @param configuration merged configuration
   * @param profileFromBase profile name from the base configuration file
   * @return finalized configuration
   */
  public static ServerConfiguration finalizeMerged(
      ServerConfiguration configuration, String profileFromBase) {
    ServerMode mode = configuration.mode();
    final String profile =
        StringHelper.isBlank(profileFromBase) ? configuration.profile() : profileFromBase;

    return new ServerConfiguration(
        profile,
        configuration.enabled(),
        mode,
        configuration.name(),
        configuration.version(),
        configuration.type(),
        configuration.instructions(),
        configuration.requestTimeout(),
        configuration.capabilities(),
        configuration.changeNotification(),
        mode == ServerMode.STREAMABLE ? configuration.streamable() : null);
  }
}
