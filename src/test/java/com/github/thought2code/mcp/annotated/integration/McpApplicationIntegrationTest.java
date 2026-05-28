package com.github.thought2code.mcp.annotated.integration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.configuration.McpConfigurationLoader;
import com.github.thought2code.mcp.annotated.configuration.McpServerConfiguration;
import com.github.thought2code.mcp.annotated.enums.ServerMode;
import com.github.thought2code.mcp.annotated.exception.McpServerConfigurationException;
import com.github.thought2code.mcp.annotated.server.AnnotatedMcpServer;
import com.github.thought2code.mcp.annotated.support.McpClientVerificationSupport;
import com.github.thought2code.mcp.annotated.support.TestMcpConfigurations;
import com.github.thought2code.mcp.annotated.support.TestMcpServerLifecycle;
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
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Tag("integration")
@Execution(ExecutionMode.SAME_THREAD)
@SuppressWarnings("deprecation")
class McpApplicationIntegrationTest {

  static McpApplicationContext context;

  private final Duration requestTimeout = Duration.ofSeconds(60);

  @BeforeAll
  static void setup() {
    context = McpApplicationContext.from(IntegrationMcpApplication.class);
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
    AnnotatedMcpServer server =
        TestMcpServerLifecycle.start(context, TestMcpConfigurations.sse(port));
    try {
      HttpClientSseClientTransport transport =
          HttpClientSseClientTransport.builder("http://localhost:" + port)
              .sseEndpoint("/sse")
              .build();

      try (McpSyncClient client =
          McpClient.sync(transport).requestTimeout(requestTimeout).build()) {
        McpClientVerificationSupport.verifyAll(client);
      }
    } finally {
      server.stop();
    }
  }

  @Test
  void streamableTransport_shouldServeAllFixtureComponents() {
    int port = new Random().nextInt(8000, 9000);
    AnnotatedMcpServer server =
        TestMcpServerLifecycle.start(context, TestMcpConfigurations.streamable(port));
    try {
      HttpClientStreamableHttpTransport transport =
          HttpClientStreamableHttpTransport.builder("http://localhost:" + port)
              .endpoint("/mcp/message")
              .build();

      try (McpSyncClient client =
          McpClient.sync(transport).requestTimeout(requestTimeout).build()) {
        McpClientVerificationSupport.verifyAll(client);
      }
    } finally {
      server.stop();
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
    AnnotatedMcpServer server =
        assertDoesNotThrow(() -> TestMcpServerLifecycle.start(context, configuration));
    assertNull(server);
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
  void sseModeConfig_shouldLoadWithoutStartingInProcessServer() {
    McpServerConfiguration configuration =
        new McpConfigurationLoader("test-mcp-server-enable-http-sse-mode.yml").loadConfig();
    assertEquals(ServerMode.SSE, configuration.mode());
  }

  @Test
  void streamableModeConfig_shouldLoadWithoutStartingInProcessServer() {
    McpServerConfiguration configuration =
        new McpConfigurationLoader("test-mcp-server-enable-streamable-http-mode.yml").loadConfig();
    assertEquals(ServerMode.STREAMABLE, configuration.mode());
  }

  @Test
  void unknownModeConfig_shouldFailDuringLoad() {
    assertThrows(
        McpServerConfigurationException.class,
        () ->
            TestMcpServerLifecycle.start(
                context,
                new McpConfigurationLoader("test-mcp-server-enable-unknown-mode.yml")
                    .loadConfig()));
  }
}
