package com.github.thought2code.mcp.annotated.enums;

/**
 * MIME types supported by {@code @McpResource} metadata.
 *
 * @author codeboyzhou
 */
public enum MimeType {

  /** Plain text resource content. */
  TEXT_PLAIN("text/plain"),

  /** JSON resource content. */
  APPLICATION_JSON("application/json");

  private final String value;

  MimeType(String value) {
    this.value = value;
  }

  /**
   * Returns the MIME type string sent to the MCP schema.
   *
   * @return the MIME type value
   */
  public String getValue() {
    return value;
  }
}
