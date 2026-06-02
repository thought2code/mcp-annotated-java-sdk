package com.github.thought2code.mcp.annotated.enums;

/**
 * MCP transport mode selected in server configuration.
 *
 * <p>Determines which {@link com.github.thought2code.mcp.annotated.server.AnnotatedMcpServer}
 * implementation {@link com.github.thought2code.mcp.annotated.McpApplication} constructs at
 * startup.
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

  /**
   * Returns a short human-readable label for this server mode.
   *
   * @return the mode description used in logs and diagnostics
   */
  public String description() {
    return description;
  }
}
