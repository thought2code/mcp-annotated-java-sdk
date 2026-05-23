package com.github.thought2code.mcp.annotated.enums;

/**
 * This enum represents the mode of MCP (Model Context Protocol) server.
 *
 * <p>It can be either {@link #STDIO}, {@link #SSE}, or {@link #STREAMABLE}.
 *
 * @author codeboyzhou
 */
public enum ServerMode {

  /** The MCP server runs in {@code STDIO} mode. */
  STDIO("Stdio"),

  /**
   * The MCP server runs in HTTP {@code SSE} mode.
   *
   * @deprecated HTTP SSE mode is deprecated; use {@link #STREAMABLE} instead.
   */
  @Deprecated(since = "0.16.0", forRemoval = true)
  SSE("HTTP SSE"),

  /** The MCP server runs in {@code STREAMABLE} http mode. */
  STREAMABLE("Streamable HTTP");

  private final String description;

  ServerMode(String description) {
    this.description = description;
  }

  public String description() {
    return description;
  }
}
