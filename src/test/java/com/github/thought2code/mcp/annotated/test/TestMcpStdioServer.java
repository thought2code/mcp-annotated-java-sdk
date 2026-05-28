package com.github.thought2code.mcp.annotated.test;

import com.github.thought2code.mcp.annotated.McpApplication;
import com.github.thought2code.mcp.annotated.integration.IntegrationMcpApplication;

public class TestMcpStdioServer {

  private static final String STDIO_CONFIG = "test-mcp-server-enable-stdio-mode.yml";

  public static void main(String[] args) {
    McpApplication.run(IntegrationMcpApplication.class, args, STDIO_CONFIG);
  }
}
