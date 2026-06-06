package com.github.thought2code.mcp.annotated.configuration;

import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.thought2code.mcp.annotated.enums.ServerMode;
import com.github.thought2code.mcp.annotated.enums.ServerType;
import com.github.thought2code.mcp.annotated.util.StringHelper;

/**
 * Root YAML configuration for an annotated MCP server application.
 *
 * <p>Loaded by {@link ConfigurationLoader} and merged with profile-specific overlays. Drives server
 * mode ({@link com.github.thought2code.mcp.annotated.enums.ServerMode}), transport endpoints, MCP
 * capability flags, and change-notification settings.
 *
 * @param profile optional profile name; when set, {@code mcp-server-{profile}.yml} is merged
 * @param enabled whether the MCP server should start
 * @param mode transport mode (stdio or streamable HTTP)
 * @param name MCP server implementation name
 * @param version MCP server implementation version
 * @param type sync vs async MCP server API style
 * @param instructions optional server instructions sent to clients
 * @param requestTimeout default request timeout in seconds
 * @param capabilities feature flags advertised during initialization
 * @param changeNotification list-change notification capability flags
 * @param streamable streamable HTTP transport settings
 * @see <a href="https://thought2code.github.io/mcp-annotated-java-sdk/getting-started">MCP
 *     Annotated Java SDK Documentation</a>
 * @author codeboyzhou
 */
public record ServerConfiguration(
    @JsonProperty("profile") String profile,
    @JsonProperty("enabled") Boolean enabled,
    @JsonProperty("mode") ServerMode mode,
    @JsonProperty("name") String name,
    @JsonProperty("version") String version,
    @JsonProperty("type") ServerType type,
    @JsonProperty("instructions") String instructions,
    @JsonProperty("request-timeout") Long requestTimeout,
    @JsonMerge @JsonProperty("capabilities") ServerCapabilities capabilities,
    @JsonMerge @JsonProperty("change-notification") ServerChangeNotification changeNotification,
    @JsonMerge @JsonProperty("streamable") ServerStreamable streamable) {

  /**
   * Creates a new instance of {@code Builder} to build {@code ServerConfiguration}.
   *
   * @return A new instance of {@code Builder}.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Mutable builder for {@link ServerConfiguration}.
   *
   * <p>Primarily used in tests; YAML files are deserialized directly into the record.
   */
  public static class Builder {
    /** The profile. */
    private String profile = StringHelper.EMPTY;

    /** The enabled status. */
    private Boolean enabled = ServerDefaults.ENABLED;

    /** The server mode. */
    private ServerMode mode = ServerDefaults.MODE;

    /** The server name. */
    private String name = ServerDefaults.NAME;

    /** The server version. */
    private String version = ServerDefaults.VERSION;

    /** The server type. */
    private ServerType type = ServerDefaults.TYPE;

    /** The server instructions. */
    private String instructions = ServerDefaults.INSTRUCTIONS;

    /** The request timeout. */
    private Long requestTimeout = ServerDefaults.REQUEST_TIMEOUT;

    /** The server capabilities. */
    private ServerCapabilities capabilities = ServerCapabilities.builder().build();

    /** The change notification configuration. */
    private ServerChangeNotification changeNotification =
        ServerChangeNotification.builder().build();

    /** The streamable configuration. */
    private ServerStreamable streamable = ServerStreamable.builder().build();

    /**
     * Sets the profile.
     *
     * @param profile The profile.
     * @return This builder instance.
     */
    public Builder profile(String profile) {
      this.profile = profile;
      return this;
    }

    /**
     * Sets the enabled status.
     *
     * @param enabled The enabled status.
     * @return This builder instance.
     */
    public Builder enabled(Boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    /**
     * Sets the server mode.
     *
     * @param mode The server mode.
     * @return This builder instance.
     */
    public Builder mode(ServerMode mode) {
      this.mode = mode;
      return this;
    }

    /**
     * Sets the server name.
     *
     * @param name The server name.
     * @return This builder instance.
     */
    public Builder name(String name) {
      this.name = name;
      return this;
    }

    /**
     * Sets the server version.
     *
     * @param version The server version.
     * @return This builder instance.
     */
    public Builder version(String version) {
      this.version = version;
      return this;
    }

    /**
     * Sets the server type.
     *
     * @param type The server type.
     * @return This builder instance.
     */
    public Builder type(ServerType type) {
      this.type = type;
      return this;
    }

    /**
     * Sets the server instructions.
     *
     * @param instructions The server instructions.
     * @return This builder instance.
     */
    public Builder instructions(String instructions) {
      this.instructions = instructions;
      return this;
    }

    /**
     * Sets the request timeout.
     *
     * @param requestTimeout The request timeout.
     * @return This builder instance.
     */
    public Builder requestTimeout(Long requestTimeout) {
      this.requestTimeout = requestTimeout;
      return this;
    }

    /**
     * Sets the server capabilities.
     *
     * @param capabilities The server capabilities.
     * @return This builder instance.
     */
    public Builder capabilities(ServerCapabilities capabilities) {
      this.capabilities = capabilities;
      return this;
    }

    /**
     * Sets the change notification configuration.
     *
     * @param changeNotification The change notification configuration.
     * @return This builder instance.
     */
    public Builder changeNotification(ServerChangeNotification changeNotification) {
      this.changeNotification = changeNotification;
      return this;
    }

    /**
     * Sets the streamable configuration.
     *
     * @param streamable The streamable configuration.
     * @return This builder instance.
     */
    public Builder streamable(ServerStreamable streamable) {
      this.streamable = streamable;
      return this;
    }

    /**
     * Builds a new instance of {@code ServerConfiguration}.
     *
     * @return A new instance of {@code ServerConfiguration}.
     */
    public ServerConfiguration build() {
      return new ServerConfiguration(
          profile,
          enabled,
          mode,
          name,
          version,
          type,
          instructions,
          requestTimeout,
          capabilities,
          changeNotification,
          streamable);
    }
  }
}
