package com.github.thought2code.mcp.annotated.support;

import com.github.thought2code.mcp.annotated.configuration.ServerCapabilities;
import com.github.thought2code.mcp.annotated.configuration.ServerChangeNotification;
import com.github.thought2code.mcp.annotated.configuration.ServerConfiguration;
import com.github.thought2code.mcp.annotated.configuration.ServerStreamable;
import com.github.thought2code.mcp.annotated.enums.ServerMode;
import com.github.thought2code.mcp.annotated.enums.ServerType;

/** Builds valid {@link ServerConfiguration} instances for unit and integration tests. */
public final class TestMcpConfigurations {

  private TestMcpConfigurations() {}

  public static ServerConfiguration.Builder baseBuilder() {
    return ServerConfiguration.builder()
        .enabled(true)
        .name("mcp-server")
        .version("1.0.0")
        .type(ServerType.SYNC)
        .instructions("test")
        .requestTimeout(60_000L)
        .capabilities(
            ServerCapabilities.builder()
                .resource(true)
                .subscribeResource(true)
                .prompt(true)
                .tool(true)
                .completion(true)
                .build())
        .changeNotification(
            ServerChangeNotification.builder().resource(true).prompt(true).tool(true).build());
  }

  public static ServerConfiguration stdio() {
    return baseBuilder().mode(ServerMode.STDIO).build();
  }

  public static ServerConfiguration streamable(int port) {
    return streamable(port, ServerType.SYNC);
  }

  public static ServerConfiguration streamableAsync(int port) {
    return streamable(port, ServerType.ASYNC);
  }

  public static ServerConfiguration streamable(int port, ServerType type) {
    return baseBuilder()
        .mode(ServerMode.STREAMABLE)
        .type(type)
        .streamable(
            ServerStreamable.builder()
                .mcpEndpoint("/mcp/message")
                .disallowDelete(false)
                .keepAliveInterval(20_000L)
                .port(port)
                .build())
        .build();
  }
}
