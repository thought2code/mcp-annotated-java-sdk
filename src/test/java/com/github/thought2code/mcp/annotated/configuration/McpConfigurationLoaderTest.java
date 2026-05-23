package com.github.thought2code.mcp.annotated.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.thought2code.mcp.annotated.enums.McpServerError;
import com.github.thought2code.mcp.annotated.enums.ServerMode;
import com.github.thought2code.mcp.annotated.enums.ServerType;
import com.github.thought2code.mcp.annotated.exception.McpServerConfigurationException;
import org.junit.jupiter.api.Test;

class McpConfigurationLoaderTest {

  @Test
  void loadConfig_shouldMergeProfileOverrides() {
    McpServerConfiguration configuration =
        new McpConfigurationLoader("test-mcp-server-with-profile.yml").loadConfig();
    assertNotNull(configuration);
    assertEquals("dev", configuration.profile());
    assertTrue(configuration.enabled());
    assertEquals(ServerMode.STREAMABLE, configuration.mode());
    assertEquals("mcp-server-dev", configuration.name());
    assertEquals("1.0.0-dev", configuration.version());
    assertEquals(ServerType.SYNC, configuration.type());
    assertEquals(60000L, configuration.requestTimeout());
    assertFalse(configuration.capabilities().resource());
    assertTrue(configuration.capabilities().prompt());
    assertTrue(configuration.capabilities().tool());
    assertFalse(configuration.changeNotification().resource());
    assertTrue(configuration.changeNotification().prompt());
    assertTrue(configuration.changeNotification().tool());
    assertEquals("/mcp/message/dev", configuration.streamable().mcpEndpoint());
    assertTrue(configuration.streamable().disallowDelete());
    assertEquals(30000L, configuration.streamable().keepAliveInterval());
    assertEquals(9004, configuration.streamable().port());
  }

  @Test
  void loadConfig_shouldFailValidationWhenMergedProfileIsInvalid() {
    McpServerConfigurationException exception =
        assertThrows(
            McpServerConfigurationException.class,
            () ->
                new McpConfigurationLoader("test-mcp-server-with-invalid-profile.yml")
                    .loadConfig());
    assertTrue(exception.getMessage().contains("subscribe-resource"));
  }

  @Test
  void loadConfig_shouldUseUnifiedErrorWhenConfigFileMissing() {
    McpServerConfigurationException exception =
        assertThrows(
            McpServerConfigurationException.class,
            () -> new McpConfigurationLoader("missing-config.yml").loadConfig());
    assertTrue(exception.getMessage().contains(McpServerError.CONFIG_FILE_NOT_FOUND.getCode()));
    assertTrue(exception.getMessage().contains("missing-config.yml"));
  }

  @Test
  void loadConfig_shouldLoadDefaultClasspathConfigWithoutProfile() {
    McpServerConfiguration configuration =
        new McpConfigurationLoader("mcp-server.yml").loadConfig();
    assertEquals(ServerMode.STREAMABLE, configuration.mode());
    assertTrue(configuration.enabled());
  }
}
