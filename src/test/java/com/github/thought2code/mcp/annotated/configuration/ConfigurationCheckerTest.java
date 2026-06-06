package com.github.thought2code.mcp.annotated.configuration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.thought2code.mcp.annotated.enums.ServerMode;
import com.github.thought2code.mcp.annotated.exception.McpServerConfigurationException;
import com.github.thought2code.mcp.annotated.support.TestMcpConfigurations;
import org.junit.jupiter.api.Test;

class ConfigurationCheckerTest {

  @Test
  void check_shouldAcceptValidStdioConfiguration() {
    assertDoesNotThrow(() -> ConfigurationChecker.check(TestMcpConfigurations.stdio()));
  }

  @Test
  void check_shouldAcceptValidStreamableConfiguration() {
    assertDoesNotThrow(() -> ConfigurationChecker.check(TestMcpConfigurations.streamable(9000)));
  }

  @Test
  void check_shouldRejectMissingName() {
    ServerConfiguration configuration =
        TestMcpConfigurations.baseBuilder().name(null).mode(ServerMode.STDIO).build();
    McpServerConfigurationException exception =
        assertThrows(
            McpServerConfigurationException.class, () -> ConfigurationChecker.check(configuration));
    assertTrue(exception.getMessage().contains("name"));
  }

  @Test
  void check_shouldRejectMissingSubscribeResourceWhenResourceEnabled() {
    ServerConfiguration configuration =
        TestMcpConfigurations.baseBuilder()
            .mode(ServerMode.STDIO)
            .capabilities(
                ServerCapabilities.builder()
                    .resource(true)
                    .subscribeResource(null)
                    .prompt(true)
                    .tool(true)
                    .completion(true)
                    .build())
            .build();
    McpServerConfigurationException exception =
        assertThrows(
            McpServerConfigurationException.class, () -> ConfigurationChecker.check(configuration));
    assertTrue(exception.getMessage().contains("capabilities.subscribe-resource"));
  }

  @Test
  void check_shouldRejectMissingStreamableSettingsWhenModeIsStreamable() {
    ServerConfiguration configuration =
        TestMcpConfigurations.baseBuilder().mode(ServerMode.STREAMABLE).streamable(null).build();
    McpServerConfigurationException exception =
        assertThrows(
            McpServerConfigurationException.class, () -> ConfigurationChecker.check(configuration));
    assertTrue(exception.getMessage().contains("streamable"));
  }
}
