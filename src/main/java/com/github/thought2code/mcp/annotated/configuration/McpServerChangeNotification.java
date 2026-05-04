package com.github.thought2code.mcp.annotated.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This record represents a change notification for MCP (Model Context Protocol) server
 * capabilities.
 *
 * <p>It contains boolean flags indicating whether the server supports resource, prompt, and tool
 * change notification.
 *
 * @author codeboyzhou
 */
public record McpServerChangeNotification(
    @JsonProperty("resource") Boolean resource,
    @JsonProperty("prompt") Boolean prompt,
    @JsonProperty("tool") Boolean tool) {

  /**
   * Creates a new instance of {@code Builder} to build {@code McpServerChangeNotification}.
   *
   * @return A new instance of {@code Builder}.
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder class for {@code McpServerChangeNotification}. */
  public static class Builder {
    /** The resource change notification flag. */
    private Boolean resource = McpServerDefaults.CHANGE_NOTIFICATION_ENABLED;

    /** The prompt change notification flag. */
    private Boolean prompt = McpServerDefaults.CHANGE_NOTIFICATION_ENABLED;

    /** The tool change notification flag. */
    private Boolean tool = McpServerDefaults.CHANGE_NOTIFICATION_ENABLED;

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
     * Builds an instance of {@code McpServerChangeNotification} with the configured values.
     *
     * @return A new instance of {@code McpServerChangeNotification}.
     */
    public McpServerChangeNotification build() {
      return new McpServerChangeNotification(resource, prompt, tool);
    }
  }
}
