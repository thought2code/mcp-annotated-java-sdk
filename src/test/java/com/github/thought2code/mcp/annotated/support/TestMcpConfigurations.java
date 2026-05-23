package com.github.thought2code.mcp.annotated.support;

import com.github.thought2code.mcp.annotated.configuration.McpServerCapabilities;
import com.github.thought2code.mcp.annotated.configuration.McpServerChangeNotification;
import com.github.thought2code.mcp.annotated.configuration.McpServerConfiguration;
import com.github.thought2code.mcp.annotated.configuration.McpServerSSE;
import com.github.thought2code.mcp.annotated.configuration.McpServerStreamable;
import com.github.thought2code.mcp.annotated.enums.ServerMode;
import com.github.thought2code.mcp.annotated.enums.ServerType;

/** Builds valid {@link McpServerConfiguration} instances for unit and integration tests. */
public final class TestMcpConfigurations {

  private TestMcpConfigurations() {}

  public static McpServerConfiguration.Builder baseBuilder() {
    return McpServerConfiguration.builder()
        .enabled(true)
        .name("mcp-server")
        .version("1.0.0")
        .type(ServerType.SYNC)
        .instructions("test")
        .requestTimeout(60_000L)
        .capabilities(
            McpServerCapabilities.builder()
                .resource(true)
                .subscribeResource(true)
                .prompt(true)
                .tool(true)
                .completion(true)
                .build())
        .changeNotification(
            McpServerChangeNotification.builder().resource(true).prompt(true).tool(true).build());
  }

  public static McpServerConfiguration stdio() {
    return baseBuilder().mode(ServerMode.STDIO).build();
  }

  public static McpServerConfiguration sse(int port) {
    return baseBuilder()
        .mode(ServerMode.SSE)
        .sse(
            McpServerSSE.builder()
                .messageEndpoint("/mcp/message")
                .endpoint("/sse")
                .baseUrl("http://localhost:" + port)
                .port(port)
                .build())
        .build();
  }

  public static McpServerConfiguration streamable(int port) {
    return baseBuilder()
        .mode(ServerMode.STREAMABLE)
        .streamable(
            McpServerStreamable.builder()
                .mcpEndpoint("/mcp/message")
                .disallowDelete(false)
                .keepAliveInterval(20_000L)
                .port(port)
                .build())
        .build();
  }
}
