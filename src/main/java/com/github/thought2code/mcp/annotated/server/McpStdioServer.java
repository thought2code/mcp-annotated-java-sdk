package com.github.thought2code.mcp.annotated.server;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.configuration.McpServerConfiguration;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;

/**
 * MCP server implementation for Standard Input/Output (STDIO) mode.
 *
 * <p>This class extends {@link McpServerBase} and provides an MCP server implementation that uses
 * standard input/output for communication. STDIO mode is the default mode for CLI tools and allows
 * the server to communicate through standard input and output streams.
 *
 * <p>STDIO mode is suitable for:
 *
 * <ul>
 *   <li>Command-line interface (CLI) applications
 *   <li>Integration with shell scripts and pipelines
 *   <li>Simple communication scenarios where HTTP is not required
 * </ul>
 *
 * <p>The server uses the standard input stream for receiving requests and the standard output
 * stream for sending responses, making it easy to integrate with existing command-line tools.
 *
 * @author codeboyzhou
 * @see McpServerBase
 * @see McpSseServer
 * @see McpStreamableServer
 * @see StdioServerTransportProvider
 */
public class McpStdioServer extends McpServerBase {
  /**
   * Constructs a new {@link McpStdioServer} with the specified configuration and application
   * context.
   *
   * @param configuration the server configuration
   * @param context the application-scoped runtime context
   */
  public McpStdioServer(McpServerConfiguration configuration, McpApplicationContext context) {
    super(configuration, context);
  }

  /**
   * Creates and returns a synchronization specification for STDIO mode.
   *
   * <p>This method creates an {@link McpServer.SyncSpecification} that uses standard input/output
   * transport provider for communication. The transport provider is configured with the default
   * JSON mapper for message serialization and deserialization.
   *
   * @return a synchronization specification configured for STDIO transport
   * @see StdioServerTransportProvider
   * @see McpJsonDefaults
   */
  @Override
  public McpServer.SyncSpecification<?> createSyncSpecification() {
    return McpServer.sync(new StdioServerTransportProvider(McpJsonDefaults.getMapper()));
  }
}
