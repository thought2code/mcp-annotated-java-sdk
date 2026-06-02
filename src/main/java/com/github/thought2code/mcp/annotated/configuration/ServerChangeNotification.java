package com.github.thought2code.mcp.annotated.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MCP list-change notification capability flags from server configuration.
 *
 * @param resource whether the server may notify clients when the resource list changes
 * @param prompt whether the server may notify clients when the prompt list changes
 * @param tool whether the server may notify clients when the tool list changes
 * @author codeboyzhou
 */
public record ServerChangeNotification(
    @JsonProperty("resource") Boolean resource,
    @JsonProperty("prompt") Boolean prompt,
    @JsonProperty("tool") Boolean tool) {

  /**
   * Creates a new instance of {@code Builder} to build {@code ServerChangeNotification}.
   *
   * @return A new instance of {@code Builder}.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Mutable builder for {@link ServerChangeNotification}.
   *
   * <p>Controls whether list-change notifications are emitted per component kind.
   */
  public static class Builder {
    /** The resource change notification flag. */
    private Boolean resource = ServerDefaults.CHANGE_NOTIFICATION_ENABLED;

    /** The prompt change notification flag. */
    private Boolean prompt = ServerDefaults.CHANGE_NOTIFICATION_ENABLED;

    /** The tool change notification flag. */
    private Boolean tool = ServerDefaults.CHANGE_NOTIFICATION_ENABLED;

    /**
     * Sets the resource change notification flag.
     *
     * @param resource The resource change notification flag.
     * @return This builder instance.
     */
    public Builder resource(Boolean resource) {
      this.resource = resource;
      return this;
    }

    /**
     * Sets the prompt change notification flag.
     *
     * @param prompt The prompt change notification flag.
     * @return This builder instance.
     */
    public Builder prompt(Boolean prompt) {
      this.prompt = prompt;
      return this;
    }

    /**
     * Sets the tool change notification flag.
     *
     * @param tool The tool change notification flag.
     * @return This builder instance.
     */
    public Builder tool(Boolean tool) {
      this.tool = tool;
      return this;
    }

    /**
     * Builds an instance of {@code ServerChangeNotification} with the configured values.
     *
     * @return A new instance of {@code ServerChangeNotification}.
     */
    public ServerChangeNotification build() {
      return new ServerChangeNotification(resource, prompt, tool);
    }
  }
}
