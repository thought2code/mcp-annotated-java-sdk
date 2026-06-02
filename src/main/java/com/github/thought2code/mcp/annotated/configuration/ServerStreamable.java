package com.github.thought2code.mcp.annotated.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Streamable HTTP transport settings for an MCP server.
 *
 * @param mcpEndpoint servlet path segment for the MCP endpoint
 * @param disallowDelete whether HTTP DELETE on the MCP session is rejected
 * @param keepAliveInterval optional SSE keep-alive interval in milliseconds
 * @param port Jetty listen port
 * @see <a href="https://thought2code.github.io/mcp-annotated-java-sdk/getting-started">MCP
 *     Annotated Java SDK Documentation</a>
 * @author codeboyzhou
 */
public record ServerStreamable(
    @JsonProperty("mcp-endpoint") String mcpEndpoint,
    @JsonProperty("disallow-delete") Boolean disallowDelete,
    @JsonProperty("keep-alive-interval") Long keepAliveInterval,
    @JsonProperty("port") Integer port) {

  /**
   * Creates a new instance of {@code Builder} to build {@code ServerStreamable}.
   *
   * @return A new instance of {@code Builder}.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Mutable builder for {@link ServerStreamable}.
   *
   * <p>Holds HTTP endpoint, session, and keep-alive settings for STREAMABLE transport.
   */
  public static class Builder {
    /** The MCP endpoint. */
    private String mcpEndpoint = ServerDefaults.STREAMABLE_MCP_ENDPOINT;

    /** The disallow delete flag. */
    private Boolean disallowDelete = ServerDefaults.STREAMABLE_DISALLOW_DELETE;

    /** The keep-alive interval. */
    private Long keepAliveInterval = ServerDefaults.STREAMABLE_KEEP_ALIVE_INTERVAL;

    /** The port. */
    private Integer port = ServerDefaults.PORT;

    /**
     * Sets the MCP endpoint.
     *
     * @param mcpEndpoint The MCP endpoint.
     * @return This builder instance.
     */
    public Builder mcpEndpoint(String mcpEndpoint) {
      this.mcpEndpoint = mcpEndpoint;
      return this;
    }

    /**
     * Sets the disallow delete flag.
     *
     * @param disallowDelete The disallow delete flag.
     * @return This builder instance.
     */
    public Builder disallowDelete(Boolean disallowDelete) {
      this.disallowDelete = disallowDelete;
      return this;
    }

    /**
     * Sets the keep-alive interval.
     *
     * @param keepAliveInterval The keep-alive interval.
     * @return This builder instance.
     */
    public Builder keepAliveInterval(Long keepAliveInterval) {
      this.keepAliveInterval = keepAliveInterval;
      return this;
    }

    /**
     * Sets the port.
     *
     * @param port The port.
     * @return This builder instance.
     */
    public Builder port(Integer port) {
      this.port = port;
      return this;
    }

    /**
     * Builds an instance of {@code ServerStreamable} with the configured values.
     *
     * @return A new instance of {@code ServerStreamable}.
     */
    public ServerStreamable build() {
      return new ServerStreamable(mcpEndpoint, disallowDelete, keepAliveInterval, port);
    }
  }
}
