package com.github.thought2code.mcp.annotated.enums;

/**
 * This enum represents the type of MCP (Model Context Protocol) server.
 *
 * <p>It can be either {@link #SYNC} or {@link #ASYNC}.
 *
 * @author codeboyzhou
 */
public enum ServerType {

  /** The MCP server runs in {@code SYNC} mode. */
  SYNC("Sync"),

  /** The MCP server runs in {@code ASYNC} mode. */
  ASYNC("Async");

  private final String description;

  ServerType(String description) {
    this.description = description;
  }

  /**
   * Returns a short human-readable label for this server type.
   *
   * @return the type description used in logs and diagnostics
   */
  public String description() {
    return description;
  }
}
