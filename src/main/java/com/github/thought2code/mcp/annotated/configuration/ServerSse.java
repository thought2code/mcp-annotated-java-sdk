package com.github.thought2code.mcp.annotated.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Server-Sent Events (SSE) configuration for an MCP server.
 *
 * @see <a href="https://thought2code.github.io/mcp-annotated-java-sdk/getting-started">MCP
 *     Annotated Java SDK Documentation</a>
 * @author codeboyzhou
 * @deprecated HTTP SSE mode is deprecated; use {@link ServerStreamable} with {@link
 *     com.github.thought2code.mcp.annotated.enums.ServerMode#STREAMABLE} instead.
 */
@Deprecated(since = "0.16.0", forRemoval = true)
public record ServerSse(
    @JsonProperty("message-endpoint") String messageEndpoint,
    @JsonProperty("endpoint") String endpoint,
    @JsonProperty("base-url") String baseUrl,
    @JsonProperty("port") Integer port) {

  /**
   * Creates a new instance of {@code Builder} to build {@code ServerSse}.
   *
   * @return A new instance of {@code Builder}.
   * @deprecated HTTP SSE mode is deprecated; use {@link ServerStreamable} instead.
   */
  @Deprecated(since = "0.16.0", forRemoval = true)
  public static Builder builder() {
    return new Builder();
  }

  /** Builder class for {@code ServerSse}. */
  @Deprecated(since = "0.16.0", forRemoval = true)
  public static class Builder {
    /** The message endpoint. */
    private String messageEndpoint = ServerDefaults.SSE_MESSAGE_ENDPOINT;

    /** The endpoint. */
    private String endpoint = ServerDefaults.SSE_ENDPOINT;

    /** The base URL. */
    private String baseUrl = ServerDefaults.SSE_BASE_URL;

    /** The port. */
    private Integer port = ServerDefaults.PORT;

    /**
     * Sets the message endpoint.
     *
     * @param messageEndpoint The message endpoint.
     * @return This builder instance.
     */
    public Builder messageEndpoint(String messageEndpoint) {
      this.messageEndpoint = messageEndpoint;
      return this;
    }

    /**
     * Sets the endpoint.
     *
     * @param endpoint The endpoint.
     * @return This builder instance.
     */
    public Builder endpoint(String endpoint) {
      this.endpoint = endpoint;
      return this;
    }

    /**
     * Sets the base URL.
     *
     * @param baseUrl The base URL.
     * @return This builder instance.
     */
    public Builder baseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
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
     * Builds an instance of {@code ServerSse} with the configured values.
     *
     * @return A new instance of {@code ServerSse}.
     * @deprecated HTTP SSE mode is deprecated; use {@link ServerStreamable} instead.
     */
    @Deprecated(since = "0.16.0", forRemoval = true)
    public ServerSse build() {
      return new ServerSse(messageEndpoint, endpoint, baseUrl, port);
    }
  }
}
