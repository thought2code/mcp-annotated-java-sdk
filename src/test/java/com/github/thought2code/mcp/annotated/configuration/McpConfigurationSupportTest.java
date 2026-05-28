package com.github.thought2code.mcp.annotated.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.github.thought2code.mcp.annotated.enums.ServerMode;
import com.github.thought2code.mcp.annotated.support.TestMcpConfigurations;
import org.junit.jupiter.api.Test;

class McpConfigurationSupportTest {

  @Test
  void finalizeMerged_shouldPreserveProfileFromBaseConfiguration() {
    McpServerConfiguration configuration =
        McpServerConfiguration.builder()
            .profile("overwritten")
            .mode(ServerMode.STREAMABLE)
            .name("merged-name")
            .build();

    McpServerConfiguration finalized = McpConfigurationSupport.finalizeMerged(configuration, "dev");

    assertEquals("dev", finalized.profile());
    assertEquals("merged-name", finalized.name());
  }

  @Test
  void finalizeMerged_shouldClearTransportSettingsForNonMatchingMode() {
    McpServerConfiguration configuration =
        TestMcpConfigurations.baseBuilder()
            .mode(ServerMode.STDIO)
            .streamable(McpServerStreamable.builder().port(9001).build())
            .build();

    McpServerConfiguration finalized = McpConfigurationSupport.finalizeMerged(configuration, "");

    assertNull(finalized.sse());
    assertNull(finalized.streamable());
  }

  @Test
  @SuppressWarnings("deprecation")
  void finalizeMerged_shouldKeepSseSettingsWhenModeIsSse() {
    McpServerConfiguration configuration = TestMcpConfigurations.sse(8081);

    McpServerConfiguration finalized = McpConfigurationSupport.finalizeMerged(configuration, "");

    assertEquals(ServerMode.SSE, finalized.mode());
    assertEquals(8081, finalized.sse().port());
    assertNull(finalized.streamable());
  }
}
