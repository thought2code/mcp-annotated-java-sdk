package com.github.thought2code.mcp.annotated.integration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.configuration.McpConfigurationLoader;
import com.github.thought2code.mcp.annotated.configuration.McpServerConfiguration;
import com.github.thought2code.mcp.annotated.enums.ServerMode;
import com.github.thought2code.mcp.annotated.exception.McpServerConfigurationException;
import com.github.thought2code.mcp.annotated.server.AnnotatedMcpServer;
import com.github.thought2code.mcp.annotated.server.McpSseServer;
import com.github.thought2code.mcp.annotated.server.McpStdioServer;
import com.github.thought2code.mcp.annotated.server.McpStreamableServer;
import com.github.thought2code.mcp.annotated.support.McpClientVerificationSupport;
import com.github.thought2code.mcp.annotated.support.TestMcpConfigurations;
import com.github.thought2code.mcp.annotated.test.TestMcpStdioServer;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonDefaults;
import java.time.Duration;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
class McpApplicationIntegrationTest {

  static McpApplicationContext context;

  private final Duration requestTimeout = Duration.ofSeconds(60);

  @BeforeAll
  static void setup() {
    System.setProperty("mcp.server.testing", "true");
    context = McpApplicationContext.from(IntegrationMcpApplication.class);
  }

  private void startServer(McpServerConfiguration configuration) {
    if (!configuration.enabled()) {
      return;
    }
    AnnotatedMcpServer mcpServer =
        switch (configuration.mode()) {
          case STDIO -> new McpStdioServer(configuration, context);
          case SSE -> new McpSseServer(configuration, context);
          case STREAMABLE -> new McpStreamableServer(configuration, context);
        };
    var syncServer = mcpServer.createSyncServer();
    mcpServer.registerComponents(syncServer);
    mcpServer.start();
  }

  @Test
  void stdioTransport_shouldServeAllFixtureComponents() {
    String classpath = System.getProperty("java.class.path");
    ServerParameters serverParameters =
        ServerParameters.builder("java")
            .args("-cp", classpath, TestMcpStdioServer.class.getName())
            .build();
    StdioClientTransport transport =
        new StdioClientTransport(serverParameters, McpJsonDefaults.getMapper());

    try (McpSyncClient client = McpClient.sync(transport).requestTimeout(requestTimeout).build()) {
      McpClientVerificationSupport.verifyAll(client);
    }
  }

  @Test
  void sseTransport_shouldServeAllFixtureComponents() {
    int port = new Random().nextInt(8000, 9000);
    startServer(TestMcpConfigurations.sse(port));

    HttpClientSseClientTransport transport =
        HttpClientSseClientTransport.builder("http://localhost:" + port)
            .sseEndpoint("/sse")
            .build();

    try (McpSyncClient client = McpClient.sync(transport).requestTimeout(requestTimeout).build()) {
      McpClientVerificationSupport.verifyAll(client);
    }
  }

  @Test
  void streamableTransport_shouldServeAllFixtureComponents() {
    int port = new Random().nextInt(8000, 9000);
    startServer(TestMcpConfigurations.streamable(port));

    HttpClientStreamableHttpTransport transport =
        HttpClientStreamableHttpTransport.builder("http://localhost:" + port)
            .endpoint("/mcp/message")
            .build();

    try (McpSyncClient client = McpClient.sync(transport).requestTimeout(requestTimeout).build()) {
      McpClientVerificationSupport.verifyAll(client);
    }
  }

  @Test
  void configurationLoader_shouldLoadDefaultClasspathConfig() {
    McpServerConfiguration configuration =
        new McpConfigurationLoader("mcp-server.yml").loadConfig();
    assertEquals(ServerMode.STREAMABLE, configuration.mode());
  }

  @Test
  void disabledConfiguration_shouldNotStartServer() {
    McpServerConfiguration configuration =
        new McpConfigurationLoader("test-mcp-server-disabled.yml").loadConfig();
    assertDoesNotThrow(() -> startServer(configuration));
    assertFalse(configuration.enabled());
  }

  /**
   * STDIO mode binds to System.in/out, starting it in-process would block or break subprocess
   * tests.
   */
  @Test
  void stdioModeConfig_shouldLoadWithoutStartingInProcessServer() {
    McpServerConfiguration configuration =
        new McpConfigurationLoader("test-mcp-server-enable-stdio-mode.yml").loadConfig();
    assertEquals(ServerMode.STDIO, configuration.mode());
  }

  @Test
  void sseModeConfig_shouldLoadAndStartWithoutError() {
    McpServerConfiguration configuration =
        new McpConfigurationLoader("test-mcp-server-enable-http-sse-mode.yml").loadConfig();
    assertDoesNotThrow(() -> startServer(configuration));
    assertEquals(ServerMode.SSE, configuration.mode());
  }

  @Test
  void streamableModeConfig_shouldLoadAndStartWithoutError() {
    McpServerConfiguration configuration =
        new McpConfigurationLoader("test-mcp-server-enable-streamable-http-mode.yml").loadConfig();
    assertDoesNotThrow(() -> startServer(configuration));
    assertEquals(ServerMode.STREAMABLE, configuration.mode());
  }

  @Test
  void unknownModeConfig_shouldFailDuringLoad() {
    assertThrows(
        McpServerConfigurationException.class,
        () ->
            startServer(
                new McpConfigurationLoader("test-mcp-server-enable-unknown-mode.yml")
                    .loadConfig()));
  }
}
