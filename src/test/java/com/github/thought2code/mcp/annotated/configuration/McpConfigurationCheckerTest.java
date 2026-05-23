package com.github.thought2code.mcp.annotated.configuration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.thought2code.mcp.annotated.enums.ServerMode;
import com.github.thought2code.mcp.annotated.exception.McpServerConfigurationException;
import com.github.thought2code.mcp.annotated.support.TestMcpConfigurations;
import org.junit.jupiter.api.Test;

class McpConfigurationCheckerTest {

  @Test
  void check_shouldAcceptValidStdioConfiguration() {
    assertDoesNotThrow(() -> McpConfigurationChecker.check(TestMcpConfigurations.stdio()));
  }

  @Test
  @SuppressWarnings("deprecation")
  void check_shouldAcceptValidSseConfiguration() {
    assertDoesNotThrow(() -> McpConfigurationChecker.check(TestMcpConfigurations.sse(8081)));
  }

  @Test
  void check_shouldAcceptValidStreamableConfiguration() {
    assertDoesNotThrow(() -> McpConfigurationChecker.check(TestMcpConfigurations.streamable(9000)));
  }

  @Test
  void check_shouldRejectMissingName() {
    McpServerConfiguration configuration =
        TestMcpConfigurations.baseBuilder().name(null).mode(ServerMode.STDIO).build();
    McpServerConfigurationException exception =
        assertThrows(
            McpServerConfigurationException.class,
            () -> McpConfigurationChecker.check(configuration));
    assertTrue(exception.getMessage().contains("name"));
  }

  @Test
  void check_shouldRejectMissingSubscribeResourceWhenResourceEnabled() {
    McpServerConfiguration configuration =
        TestMcpConfigurations.baseBuilder()
            .mode(ServerMode.STDIO)
            .capabilities(
                McpServerCapabilities.builder()
                    .resource(true)
                    .subscribeResource(null)
                    .prompt(true)
                    .tool(true)
                    .completion(true)
                    .build())
            .build();
    McpServerConfigurationException exception =
        assertThrows(
            McpServerConfigurationException.class,
            () -> McpConfigurationChecker.check(configuration));
    assertTrue(exception.getMessage().contains("subscribe-resource"));
  }

  @Test
  @SuppressWarnings("deprecation")
  void check_shouldRejectMissingSseSettingsWhenModeIsSse() {
    McpServerConfiguration configuration =
        TestMcpConfigurations.baseBuilder().mode(ServerMode.SSE).sse(null).build();
    McpServerConfigurationException exception =
        assertThrows(
            McpServerConfigurationException.class,
            () -> McpConfigurationChecker.check(configuration));
    assertTrue(exception.getMessage().contains("sse"));
  }

  @Test
  void checkNull_shouldRejectWhenBothValuesAreNull() {
    McpServerConfigurationException exception =
        assertThrows(
            McpServerConfigurationException.class,
            () -> McpConfigurationChecker.checkNull("mode", null, null));
    assertTrue(exception.getMessage().contains("mode"));
  }

  @Test
  void checkBlank_shouldRejectWhenBothValuesAreBlank() {
    McpServerConfigurationException exception =
        assertThrows(
            McpServerConfigurationException.class,
            () -> McpConfigurationChecker.checkBlank("name", "  ", null));
    assertTrue(exception.getMessage().contains("name"));
  }
}
