package com.github.thought2code.mcp.annotated.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.thought2code.mcp.annotated.enums.ServerMode;
import com.github.thought2code.mcp.annotated.enums.ServerType;
import com.github.thought2code.mcp.annotated.exception.McpServerConfigurationException;
import com.github.thought2code.mcp.annotated.support.TestMcpConfigurations;
import org.junit.jupiter.api.Test;

class McpConfigurationMergerTest {

  @Test
  void merge_shouldPreferProfileValuesOverBaseValues() {
    McpServerConfiguration base = TestMcpConfigurations.streamable(9000);
    McpServerConfiguration profile =
        McpServerConfiguration.builder()
            .enabled(false)
            .name("profile-name")
            .version("2.0.0")
            .type(ServerType.ASYNC)
            .instructions("profile-instructions")
            .requestTimeout(30_000L)
            .capabilities(McpServerCapabilities.builder().resource(false).build())
            .changeNotification(McpServerChangeNotification.builder().resource(false).build())
            .streamable(McpServerStreamable.builder().port(9001).build())
            .build();

    McpServerConfiguration merged = McpConfigurationMerger.merge(base, profile);

    assertEquals(base.profile(), merged.profile());
    assertEquals(false, merged.enabled());
    assertEquals(ServerMode.STREAMABLE, merged.mode());
    assertEquals("profile-name", merged.name());
    assertEquals("2.0.0", merged.version());
    assertEquals(ServerType.ASYNC, merged.type());
    assertEquals("profile-instructions", merged.instructions());
    assertEquals(30_000L, merged.requestTimeout());
    assertEquals(false, merged.capabilities().resource());
    assertEquals(false, merged.changeNotification().resource());
    assertEquals(9001, merged.streamable().port());
  }

  @Test
  void merge_shouldReturnNullTransportSettingsForNonMatchingMode() {
    McpServerConfiguration base = TestMcpConfigurations.stdio();
    McpServerConfiguration profile =
        McpServerConfiguration.builder().mode(ServerMode.STDIO).name("profile").build();

    McpServerConfiguration merged = McpConfigurationMerger.merge(base, profile);

    assertNull(merged.sse());
    assertNull(merged.streamable());
  }

  @Test
  void merge_shouldMergeSseSettingsWhenModeIsSse() {
    McpServerConfiguration base = TestMcpConfigurations.sse(8080);
    McpServerConfiguration profile =
        McpServerConfiguration.builder()
            .mode(ServerMode.SSE)
            .sse(McpServerSSE.builder().port(8081).baseUrl("http://localhost:8081").build())
            .build();

    McpServerConfiguration merged = McpConfigurationMerger.merge(base, profile);

    assertEquals(ServerMode.SSE, merged.mode());
    assertEquals(8081, merged.sse().port());
    assertEquals("http://localhost:8081", merged.sse().baseUrl());
    assertEquals("/mcp/message", merged.sse().messageEndpoint());
    assertEquals("/sse", merged.sse().endpoint());
  }

  @Test
  void merge_shouldFailWhenRequiredCapabilityMissingInBothConfigs() {
    McpServerConfiguration base =
        TestMcpConfigurations.baseBuilder()
            .mode(ServerMode.STDIO)
            .capabilities(McpServerCapabilities.builder().resource(null).build())
            .build();
    McpServerConfiguration profile =
        McpServerConfiguration.builder()
            .capabilities(McpServerCapabilities.builder().resource(null).build())
            .build();

    McpServerConfigurationException exception =
        assertThrows(
            McpServerConfigurationException.class,
            () -> McpConfigurationMerger.merge(base, profile));
    assertTrue(exception.getMessage().contains("capabilities.resource"));
  }
}
