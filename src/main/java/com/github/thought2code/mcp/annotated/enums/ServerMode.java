package com.github.thought2code.mcp.annotated.enums;

import com.github.thought2code.mcp.annotated.McpApplication;
import com.github.thought2code.mcp.annotated.server.AnnotatedMcpServer;

/**
 * MCP transport mode selected in server configuration.
 *
 * <p>Determines which {@link AnnotatedMcpServer} implementation {@link McpApplication} constructs
 * at startup.
 *
 * @author codeboyzhou
 */
public enum ServerMode {

  /** The MCP server runs in {@code STDIO} mode. */
  STDIO("Stdio"),

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
