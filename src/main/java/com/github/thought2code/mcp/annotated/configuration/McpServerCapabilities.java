package com.github.thought2code.mcp.annotated.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This record represents the capabilities of an MCP (Model Context Protocol) server.
 *
 * @author codeboyzhou
 */
public record McpServerCapabilities(
    @JsonProperty("resource") Boolean resource,
    @JsonProperty("subscribe-resource") Boolean subscribeResource,
    @JsonProperty("prompt") Boolean prompt,
    @JsonProperty("tool") Boolean tool,
    @JsonProperty("completion") Boolean completion) {

  /**
   * Creates a new instance of {@code Builder} to build {@code McpServerCapabilities}.
   *
   * @return A new instance of {@code Builder}.
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder class for {@code McpServerCapabilities}. */
  public static class Builder {
    /** The resource capability. */
    private Boolean resource = McpServerDefaults.CAPABILITY_ENABLED;

    /** The subscribe-resource capability. */
    private Boolean subscribeResource = McpServerDefaults.CAPABILITY_ENABLED;

    /** The prompt capability. */
    private Boolean prompt = McpServerDefaults.CAPABILITY_ENABLED;

    /** The tool capability. */
    private Boolean tool = McpServerDefaults.CAPABILITY_ENABLED;

    /** The completion capability. */
    private Boolean completion = McpServerDefaults.CAPABILITY_ENABLED;

    /**
     * Sets the resource capability.
     *
     * @param resource The resource capability.
     * @return This builder instance.
     */
    public Builder resource(Boolean resource) {
      this.resource = resource;
      return this;
    }

    /**
     * Sets the subscribe-resource capability.
     *
     * @param subscribeResource The subscribe-resource capability.
     * @return This builder instance.
     */
    public Builder subscribeResource(Boolean subscribeResource) {
      this.subscribeResource = subscribeResource;
      return this;
    }

    /**
     * Sets the prompt capability.
     *
     * @param prompt The prompt capability.
     * @return This builder instance.
     */
    public Builder prompt(Boolean prompt) {
      this.prompt = prompt;
      return this;
    }

    /**
     * Sets the tool capability.
     *
     * @param tool The tool capability.
     * @return This builder instance.
     */
    public Builder tool(Boolean tool) {
      this.tool = tool;
      return this;
    }

    /**
     * Sets the completion capability.
     *
     * @param completion The completion capability.
     * @return This builder instance.
     */
    public Builder completion(Boolean completion) {
      this.completion = completion;
      return this;
    }

    /**
     * Builds an instance of {@code McpServerCapabilities} with the configured values.
     *
     * @return A new instance of {@code McpServerCapabilities}.
     */
    public McpServerCapabilities build() {
      return new McpServerCapabilities(resource, subscribeResource, prompt, tool, completion);
    }
  }
}
