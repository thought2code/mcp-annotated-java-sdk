package com.github.thought2code.mcp.annotated.test;

import com.github.thought2code.mcp.annotated.McpApplication;

public class TestMcpStdioServer {

  private static final String STDIO_CONFIG = "test-mcp-server-enable-stdio-mode.yml";

  public static void main(String[] args) {
    McpApplication.run(TestMcpStdioServer.class, args, STDIO_CONFIG);
  }
}
