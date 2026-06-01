package com.github.thought2code.mcp.annotated.integration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.configuration.ConfigurationLoader;
import com.github.thought2code.mcp.annotated.configuration.ServerConfiguration;
import com.github.thought2code.mcp.annotated.enums.McpServerError;
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
import io.modelcontextprotocol.spec.McpSchema;
import java.io.EOFException;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Hooks;

@Tag("integration")
@Execution(ExecutionMode.SAME_THREAD)
@SuppressWarnings("deprecation")
class McpApplicationIntegrationTest {

  private static final Logger log = LoggerFactory.getLogger(McpApplicationIntegrationTest.class);

  static McpApplicationContext context;

  /**
   * Some HTTP client transports may emit dropped EOF/connection-close errors while test servers are
   * shutting down. These are expected teardown-time signals and should not fail integration tests.
   */
  private static final Predicate<Throwable> EXPECTED_DROPPED_ERROR =
      error ->
          hasCause(
                  error,
                  cause ->
                      cause instanceof IOException
                          && cause.getMessage() != null
                          && cause
                              .getMessage()
                              .contains("HTTP/1.1 header parser received no bytes"))
              || hasCause(error, cause -> cause instanceof EOFException);

  private final Duration requestTimeout = Duration.ofSeconds(60);

  @BeforeAll
  static void setup() {
    Hooks.onErrorDropped(
        error -> {
          if (!EXPECTED_DROPPED_ERROR.test(error)) {
            log.error("Unexpected dropped error during integration test teardown", error);
          }
        });
    context = McpApplicationContext.from(IntegrationMcpApplication.class);
  }

  @AfterAll
  static void cleanup() {
    Hooks.resetOnErrorDropped();
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
      assert server != null;
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
      assert server != null;
      server.stop();
    }
  }

  @Test
  void toolInvocationFailure_shouldKeepClientContractAndEmitSourceMethodLog() {
    int port = new Random().nextInt(8000, 9000);
    ListAppender<ILoggingEvent> appender = attachInMemoryLogAppender();
    AnnotatedMcpServer server =
        TestMcpServerLifecycle.start(context, TestMcpConfigurations.streamable(port));
    try {
      HttpClientStreamableHttpTransport transport =
          HttpClientStreamableHttpTransport.builder("http://localhost:" + port)
              .endpoint("/mcp/message")
              .build();

      try (McpSyncClient client =
          McpClient.sync(transport).requestTimeout(requestTimeout).build()) {
        client.initialize();
        McpSchema.CallToolRequest request =
            McpSchema.CallToolRequest.builder("tool_with_exception").arguments(Map.of()).build();
        McpSchema.CallToolResult result = client.callTool(request);
        McpSchema.TextContent content = (McpSchema.TextContent) result.content().get(0);

        assertTrue(result.isError());
        assertEquals(McpServerError.METHOD_INVOCATION_ERROR.toString(), content.text());
      }

      assertTrue(
          hasInvocationFailureLog(appender.list),
          "Expected invocation failure log with sourceMethod and exception detail");
    } finally {
      assert server != null;
      server.stop();
      detachInMemoryLogAppender(appender);
    }
  }

  @Test
  void configurationLoader_shouldLoadDefaultClasspathConfig() {
    ServerConfiguration configuration = new ConfigurationLoader("mcp-server.yml").loadConfig();
    assertEquals(ServerMode.STREAMABLE, configuration.mode());
  }

  @Test
  void disabledConfiguration_shouldNotStartServer() {
    ServerConfiguration configuration =
        new ConfigurationLoader("test-mcp-server-disabled.yml").loadConfig();
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
    ServerConfiguration configuration =
        new ConfigurationLoader("test-mcp-server-enable-stdio-mode.yml").loadConfig();
    assertEquals(ServerMode.STDIO, configuration.mode());
  }

  @Test
  void sseModeConfig_shouldLoadWithoutStartingInProcessServer() {
    ServerConfiguration configuration =
        new ConfigurationLoader("test-mcp-server-enable-http-sse-mode.yml").loadConfig();
    assertEquals(ServerMode.SSE, configuration.mode());
  }

  @Test
  void streamableModeConfig_shouldLoadWithoutStartingInProcessServer() {
    ServerConfiguration configuration =
        new ConfigurationLoader("test-mcp-server-enable-streamable-http-mode.yml").loadConfig();
    assertEquals(ServerMode.STREAMABLE, configuration.mode());
  }

  @Test
  void unknownModeConfig_shouldFailDuringLoad() {
    assertThrows(
        McpServerConfigurationException.class,
        () ->
            TestMcpServerLifecycle.start(
                context,
                new ConfigurationLoader("test-mcp-server-enable-unknown-mode.yml").loadConfig()));
  }

  private static boolean hasCause(Throwable error, Predicate<Throwable> matcher) {
    Throwable current = error;
    while (current != null) {
      if (matcher.test(current)) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }

  private static ListAppender<ILoggingEvent> attachInMemoryLogAppender() {
    ch.qos.logback.classic.Logger rootLogger =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    rootLogger.addAppender(appender);
    return appender;
  }

  private static void detachInMemoryLogAppender(ListAppender<ILoggingEvent> appender) {
    ch.qos.logback.classic.Logger rootLogger =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    rootLogger.detachAppender(appender);
    appender.stop();
  }

  private static boolean hasInvocationFailureLog(List<ILoggingEvent> events) {
    for (ILoggingEvent event : events) {
      String message = event.getFormattedMessage();
      boolean sourceMethodMessage =
          message != null && message.contains("Tool invocation failed for sourceMethod=");
      boolean hasExceptionDetail =
          event.getThrowableProxy() != null
              && event.getThrowableProxy().getMessage() != null
              && event.getThrowableProxy().getMessage().contains("sensitive tool failure detail");
      if (sourceMethodMessage && hasExceptionDetail) {
        return true;
      }
    }
    return false;
  }
}
