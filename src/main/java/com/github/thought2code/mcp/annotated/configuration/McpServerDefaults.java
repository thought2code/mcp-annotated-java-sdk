package com.github.thought2code.mcp.annotated.configuration;

import com.github.thought2code.mcp.annotated.enums.ServerMode;
import com.github.thought2code.mcp.annotated.enums.ServerType;
import com.github.thought2code.mcp.annotated.util.StringHelper;
import java.time.Duration;

/**
 * Centralized constants for MCP server default configuration values.
 *
 * <p>This class consolidates all default values used across the MCP server configuration system to
 * ensure consistency and make maintenance easier. All default values for server settings,
 * capabilities, transport modes, and HTTP endpoints are defined here.
 *
 * <p>Using centralized constants prevents:
 *
 * <ul>
 *   <li>Inconsistent defaults across different configuration builders
 *   <li>Magic strings and numbers scattered throughout the codebase
 *   <li>Difficulty in updating default values when requirements change
 * </ul>
 *
 * @author codeboyzhou
 * @see McpServerConfiguration
 * @see McpServerCapabilities
 * @see McpServerChangeNotification
 * @see McpServerSSE
 * @see McpServerStreamable
 */
public final class McpServerDefaults {

  private McpServerDefaults() {
    throw new UnsupportedOperationException("Utility class should not be instantiated");
  }

  /** Default file name for the MCP server configuration file. */
  public static final String CONFIG_FILE_NAME = "mcp-server.yml";

  /** Default server enabled status. */
  public static final boolean ENABLED = true;

  /** Default server mode. */
  public static final ServerMode MODE = ServerMode.STREAMABLE;

  /** Default server name. */
  public static final String NAME = "mcp-server";

  /** Default server version. */
  public static final String VERSION = "1.0.0";

  /** Default server type. */
  public static final ServerType TYPE = ServerType.SYNC;

  /** Default server instructions (empty). */
  public static final String INSTRUCTIONS = StringHelper.EMPTY;

  /** Default request timeout in milliseconds (20 seconds). */
  public static final long REQUEST_TIMEOUT = Duration.ofSeconds(20).toMillis();

  /** Default capability enabled status. */
  public static final boolean CAPABILITY_ENABLED = true;

  /** Default change notification enabled status. */
  public static final boolean CHANGE_NOTIFICATION_ENABLED = true;

  /** Default SSE message endpoint path. */
  public static final String SSE_MESSAGE_ENDPOINT = "/mcp/message";

  /** Default SSE endpoint path. */
  public static final String SSE_ENDPOINT = "/sse";

  /** Default SSE base URL (empty). */
  public static final String SSE_BASE_URL = StringHelper.EMPTY;

  /** Default HTTP server port. */
  public static final int PORT = 8080;

  /** Default streamable MCP endpoint path. */
  public static final String STREAMABLE_MCP_ENDPOINT = "/mcp/message";

  /** Default streamable disallow delete flag. */
  public static final boolean STREAMABLE_DISALLOW_DELETE = false;

  /** Default streamable keep-alive interval in milliseconds (20 seconds). */
  public static final long STREAMABLE_KEEP_ALIVE_INTERVAL = Duration.ofSeconds(20).toMillis();
}
