package com.github.thought2code.mcp.annotated;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.thought2code.mcp.annotated.integration.IntegrationMcpApplication;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.SAME_THREAD)
class McpApplicationTest {

  @BeforeEach
  void enableTestingMode() {
    System.setProperty("mcp.server.testing", "true");
  }

  @Test
  void constructor_shouldRejectInstantiation() throws Exception {
    Constructor<McpApplication> constructor = McpApplication.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    InvocationTargetException exception =
        assertThrows(InvocationTargetException.class, constructor::newInstance);

    assertInstanceOf(UnsupportedOperationException.class, exception.getCause());
    assertTrue(
        exception
            .getCause()
            .getMessage()
            .contains("Main application class should not be instantiated"));
  }

  @Test
  void run_shouldNoOpWhenServerDisabled() {
    assertDoesNotThrow(
        () ->
            McpApplication.run(
                IntegrationMcpApplication.class,
                new String[] {"test"},
                "test-mcp-server-disabled.yml"));
  }

  @Test
  void run_shouldStartStdioSyncServer() {
    assertDoesNotThrow(
        () ->
            McpApplication.run(
                IntegrationMcpApplication.class,
                new String[] {"test"},
                "test-mcp-server-enable-stdio-mode.yml"));
  }

  @Test
  void run_shouldStartStdioAsyncServer() {
    assertDoesNotThrow(
        () ->
            McpApplication.run(
                IntegrationMcpApplication.class,
                new String[] {"test"},
                "test-mcp-server-enable-stdio-async-mode.yml"));
  }

  @Test
  void run_shouldStartStreamableServer() {
    assertDoesNotThrow(
        () ->
            McpApplication.run(
                IntegrationMcpApplication.class,
                new String[] {"test"},
                "test-mcp-server-enable-streamable-http-mode.yml"));
  }

  @Test
  @SuppressWarnings("deprecation")
  void run_shouldStartSseServer() {
    assertDoesNotThrow(
        () ->
            McpApplication.run(
                IntegrationMcpApplication.class,
                new String[] {"test"},
                "test-mcp-server-enable-http-sse-mode.yml"));
  }

  @Test
  void runWithDefaultConfigFile_shouldStartServerFromClasspathConfig() {
    assertDoesNotThrow(
        () -> McpApplication.run(IntegrationMcpApplication.class, new String[] {"test"}));
  }
}
