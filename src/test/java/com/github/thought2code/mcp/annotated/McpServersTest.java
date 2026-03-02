package com.github.thought2code.mcp.annotated;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.thought2code.mcp.annotated.configuration.McpConfigurationLoader;
import com.github.thought2code.mcp.annotated.configuration.McpServerConfiguration;
import com.github.thought2code.mcp.annotated.configuration.McpServerSSE;
import com.github.thought2code.mcp.annotated.configuration.McpServerStreamable;
import com.github.thought2code.mcp.annotated.enums.JavaTypeToJsonSchemaMapper;
import com.github.thought2code.mcp.annotated.enums.ServerMode;
import com.github.thought2code.mcp.annotated.exception.McpServerConfigurationException;
import com.github.thought2code.mcp.annotated.server.McpStructuredContent;
import com.github.thought2code.mcp.annotated.test.TestMcpStdioServer;
import com.github.thought2code.mcp.annotated.test.TestMcpToolsStructuredContent;
import com.github.thought2code.mcp.annotated.util.StringHelper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.spec.McpSchema;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class McpServersTest {

  McpServers servers = McpServers.run(McpServersTest.class, new String[] {});

  Duration requestTimeout = Duration.ofSeconds(60);

  @BeforeAll
  static void setup() {
    System.setProperty("mcp.server.testing", "true");
  }

  @Test
  void testStartStdioServer_shouldSucceed() {
    TestMcpStdioServer.main(new String[] {}); // just for jacoco coverage report

    String classpath = System.getProperty("java.class.path");

    ServerParameters serverParameters =
        ServerParameters.builder("java")
            .args("-cp", classpath, TestMcpStdioServer.class.getName())
            .build();

    StdioClientTransport transport =
        new StdioClientTransport(serverParameters, McpJsonDefaults.getMapper());

    try (McpSyncClient client = McpClient.sync(transport).requestTimeout(requestTimeout).build()) {
      Executors.newSingleThreadExecutor().execute(() -> verify(client));
    }
  }

  @Test
  void testStartSseServer_shouldSucceed() {
    final int port = new Random().nextInt(8000, 9000);

    McpServerConfiguration.Builder configuration =
        McpServerConfiguration.builder()
            .name("mcp-server")
            .version("1.0.0")
            .instructions("test")
            .requestTimeout(requestTimeout.toMillis())
            .sse(McpServerSSE.builder().baseUrl("http://localhost:" + port).port(port).build());

    HttpClientSseClientTransport transport =
        HttpClientSseClientTransport.builder("http://localhost:" + port)
            .sseEndpoint("/sse")
            .build();

    servers.startSseServer(configuration);

    try (McpSyncClient client = McpClient.sync(transport).requestTimeout(requestTimeout).build()) {
      verify(client);
    }
  }

  @Test
  void testStartStreamableServer_shouldSucceed() {
    final int port = new Random().nextInt(8000, 9000);

    McpServerConfiguration.Builder configuration =
        McpServerConfiguration.builder()
            .name("mcp-server")
            .version("1.0.0")
            .instructions("test")
            .requestTimeout(requestTimeout.toMillis())
            .streamable(McpServerStreamable.builder().port(port).build());

    HttpClientStreamableHttpTransport transport =
        HttpClientStreamableHttpTransport.builder("http://localhost:" + port)
            .endpoint("/mcp/message")
            .build();

    servers.startStreamableServer(configuration);

    try (McpSyncClient client = McpClient.sync(transport).requestTimeout(requestTimeout).build()) {
      verify(client);
    }
  }

  @Test
  void testStartServer_useDefaultConfigFileName_shouldSucceed() {
    String configFileName = "mcp-server.yml";
    McpConfigurationLoader configLoader = new McpConfigurationLoader(configFileName);
    McpServerConfiguration configuration = configLoader.loadConfig();
    assertEquals(ServerMode.STREAMABLE, configuration.mode());
    assertDoesNotThrow(() -> servers.startServer());
  }

  @Test
  void testStartServer_disabledMCP_shouldSucceed() {
    String configFileName = "test-mcp-server-disabled.yml";
    McpConfigurationLoader configLoader = new McpConfigurationLoader(configFileName);
    McpServerConfiguration configuration = configLoader.loadConfig();
    assertDoesNotThrow(() -> servers.startServer(configFileName));
    assertFalse(configuration.enabled());
  }

  @Test
  void testStartServer_enableStdioMode_shouldSucceed() {
    String configFileName = "test-mcp-server-enable-stdio-mode.yml";
    McpConfigurationLoader configLoader = new McpConfigurationLoader(configFileName);
    McpServerConfiguration configuration = configLoader.loadConfig();
    assertDoesNotThrow(() -> servers.startServer(configFileName));
    assertEquals(ServerMode.STDIO, configuration.mode());
  }

  @Test
  void testStartServer_enableHttpSseMode_shouldSucceed() {
    String configFileName = "test-mcp-server-enable-http-sse-mode.yml";
    McpConfigurationLoader configLoader = new McpConfigurationLoader(configFileName);
    McpServerConfiguration configuration = configLoader.loadConfig();
    assertDoesNotThrow(() -> servers.startServer(configFileName));
    assertEquals(ServerMode.SSE, configuration.mode());
  }

  @Test
  void testStartServer_enableStreamableHttpMode_shouldSucceed() {
    String configFileName = "test-mcp-server-enable-streamable-http-mode.yml";
    McpConfigurationLoader configLoader = new McpConfigurationLoader(configFileName);
    McpServerConfiguration configuration = configLoader.loadConfig();
    assertDoesNotThrow(() -> servers.startServer(configFileName));
    assertEquals(ServerMode.STREAMABLE, configuration.mode());
  }

  @Test
  void testStartServer_enableUnknownMode_shouldThrowException() {
    String configFileName = "test-mcp-server-enable-unknown-mode.yml";
    assertThrows(McpServerConfigurationException.class, () -> servers.startServer(configFileName));
  }

  private void verify(McpSyncClient client) {
    verifyServerInfo(client);
    verifyResourcesRegistered(client);
    verifyPromptsRegistered(client);
    verifyToolsRegistered(client);
    verifyPromptsCalled(client);
    verifyToolsCalled(client);
  }

  private void verifyServerInfo(McpSyncClient client) {
    McpSchema.InitializeResult initialized = client.initialize();
    assertEquals("mcp-server", initialized.serverInfo().name());
    assertEquals("1.0.0", initialized.serverInfo().version());
    assertEquals("test", initialized.instructions());
  }

  private void verifyResourcesRegistered(McpSyncClient client) {
    List<McpSchema.Resource> resources = client.listResources().resources();
    assertEquals(2, resources.size());

    verifyResourceRegistered(
        resources,
        "test://resource1",
        "resource1_name",
        "resource1_title",
        "resource1_description");
    verifyResourceRegistered(
        resources,
        "test://resource2",
        "resource2_name",
        "resource2_title",
        "resource2_description");
  }

  private void verifyResourceRegistered(
      List<McpSchema.Resource> resources,
      String resourceUri,
      String resourceName,
      String resourceTitle,
      String resourceDescription) {

    McpSchema.Resource resource =
        resources.stream().filter(r -> r.uri().equals(resourceUri)).findAny().orElse(null);
    assertNotNull(resource);
    assertEquals(resourceUri, resource.uri());
    assertEquals(resourceName, resource.name());
    assertEquals(resourceTitle, resource.title());
    assertEquals(resourceDescription, resource.description());
  }

  private void verifyResourcesCalled(McpSyncClient client) {
    verifyResourceCalled(client, "test://resource1", "text/plain", "resource1_content");
    verifyResourceCalled(client, "test://resource2", "text/plain", "resource2_content");
  }

  private void verifyResourceCalled(
      McpSyncClient client, String resourceUri, String resourceMimeType, String resourceContent) {

    McpSchema.ReadResourceRequest request = new McpSchema.ReadResourceRequest(resourceUri);
    McpSchema.ReadResourceResult result = client.readResource(request);
    McpSchema.TextResourceContents content =
        (McpSchema.TextResourceContents) result.contents().get(0);
    assertNotNull(content);
    assertEquals(resourceUri, content.uri());
    assertEquals(resourceMimeType, content.mimeType());
    assertEquals(resourceContent, content.text());
  }

  private void verifyPromptsRegistered(McpSyncClient client) {
    List<McpSchema.Prompt> prompts = client.listPrompts().prompts();
    assertEquals(10, prompts.size());

    verifyPromptRegistered(prompts, "promptWithDefaultName", "title", "description", 0);
    verifyPromptRegistered(
        prompts, "promptWithDefaultTitle", "promptWithDefaultTitle", "description", 0);
    verifyPromptRegistered(
        prompts, "promptWithDefaultDescription", "title", "promptWithDefaultDescription", 0);
    verifyPromptRegistered(
        prompts, "promptWithAllDefault", "promptWithAllDefault", "promptWithAllDefault", 0);
    verifyPromptRegistered(
        prompts,
        "promptWithOptionalParam",
        "promptWithOptionalParam",
        "promptWithOptionalParam",
        1);
    verifyPromptRegistered(
        prompts,
        "promptWithRequiredParam",
        "promptWithRequiredParam",
        "promptWithRequiredParam",
        1);
    verifyPromptRegistered(
        prompts, "promptWithMultiParams", "promptWithMultiParams", "promptWithMultiParams", 2);
    verifyPromptRegistered(
        prompts, "promptWithMixedParams", "promptWithMixedParams", "promptWithMixedParams", 1);
    verifyPromptRegistered(
        prompts, "promptWithVoidReturn", "promptWithVoidReturn", "promptWithVoidReturn", 0);
    verifyPromptRegistered(
        prompts, "promptWithReturnNull", "promptWithReturnNull", "promptWithReturnNull", 0);
  }

  private void verifyPromptRegistered(
      List<McpSchema.Prompt> prompts,
      String promptName,
      String promptTitle,
      String promptDescription,
      int promptArgumentsSize) {

    McpSchema.Prompt prompt =
        prompts.stream().filter(p -> p.name().equals(promptName)).findAny().orElse(null);
    assertNotNull(prompt);
    assertEquals(promptName, prompt.name());
    assertEquals(promptTitle, prompt.title());
    assertEquals(promptDescription, prompt.description());
    assertEquals(promptArgumentsSize, prompt.arguments().size());
  }

  private void verifyPromptsCalled(McpSyncClient client) {
    verifyPromptCalled(
        client, "promptWithDefaultName", Map.of(), "promptWithDefaultName is called");
    verifyPromptCalled(
        client, "promptWithDefaultTitle", Map.of(), "promptWithDefaultTitle is called");
    verifyPromptCalled(
        client, "promptWithDefaultDescription", Map.of(), "promptWithDefaultDescription is called");
    verifyPromptCalled(client, "promptWithAllDefault", Map.of(), "promptWithAllDefault is called");
    verifyPromptCalled(
        client,
        "promptWithOptionalParam",
        Map.of("param", "value"),
        "promptWithOptionalParam is called with param: value");
    verifyPromptCalled(
        client,
        "promptWithRequiredParam",
        Map.of("param", "value"),
        "promptWithRequiredParam is called with param: value");
    verifyPromptCalled(
        client,
        "promptWithMultiParams",
        Map.of("param1", "value1", "param2", "value2"),
        "promptWithMultiParams is called with params: value1, value2");
    verifyPromptCalled(
        client,
        "promptWithMixedParams",
        Map.of("mcpParam", "value"),
        "promptWithMixedParams is called with params: value, " + StringHelper.EMPTY);
    verifyPromptCalled(
        client,
        "promptWithVoidReturn",
        Map.of(),
        "The method call succeeded but has a void return type");
    verifyPromptCalled(
        client,
        "promptWithReturnNull",
        Map.of(),
        "The method call succeeded but the return value is null");
  }

  private void verifyPromptCalled(
      McpSyncClient client, String promptName, Map<String, Object> params, String expectedResult) {

    McpSchema.GetPromptRequest request = new McpSchema.GetPromptRequest(promptName, params);
    McpSchema.GetPromptResult result = client.getPrompt(request);
    McpSchema.TextContent content = (McpSchema.TextContent) result.messages().get(0).content();
    assertEquals(expectedResult, content.text());
  }

  private void verifyToolsRegistered(McpSyncClient client) {
    List<McpSchema.Tool> tools = client.listTools().tools();
    assertEquals(22, tools.size());

    verifyToolRegistered(tools, "toolWithDefaultName", "title", "description", Map.of());
    verifyToolRegistered(
        tools, "toolWithDefaultTitle", "toolWithDefaultTitle", "description", Map.of());
    verifyToolRegistered(
        tools, "toolWithDefaultDescription", "title", "toolWithDefaultDescription", Map.of());
    verifyToolRegistered(
        tools, "toolWithAllDefault", "toolWithAllDefault", "toolWithAllDefault", Map.of());
    verifyToolRegistered(
        tools,
        "toolWithOptionalParam",
        "toolWithOptionalParam",
        "toolWithOptionalParam",
        Map.of("param", String.class));
    verifyToolRegistered(
        tools,
        "toolWithRequiredParam",
        "toolWithRequiredParam",
        "toolWithRequiredParam",
        Map.of("param", String.class));
    verifyToolRegistered(
        tools,
        "toolWithMultiParams",
        "toolWithMultiParams",
        "toolWithMultiParams",
        Map.of("param1", String.class, "param2", String.class));
    verifyToolRegistered(
        tools,
        "toolWithMixedParams",
        "toolWithMixedParams",
        "toolWithMixedParams",
        Map.of("mcpParam", String.class));
    verifyToolRegistered(
        tools, "toolWithVoidReturn", "toolWithVoidReturn", "toolWithVoidReturn", Map.of());
    verifyToolRegistered(
        tools, "toolWithReturnNull", "toolWithReturnNull", "toolWithReturnNull", Map.of());
    verifyToolRegistered(
        tools,
        "toolWithIntParam",
        "toolWithIntParam",
        "toolWithIntParam",
        Map.of("param", int.class));
    verifyToolRegistered(
        tools,
        "toolWithIntegerParam",
        "toolWithIntegerParam",
        "toolWithIntegerParam",
        Map.of("param", Integer.class));
    verifyToolRegistered(
        tools,
        "toolWithLongParam",
        "toolWithLongParam",
        "toolWithLongParam",
        Map.of("param", long.class));
    verifyToolRegistered(
        tools,
        "toolWithLongClassParam",
        "toolWithLongClassParam",
        "toolWithLongClassParam",
        Map.of("param", Long.class));
    verifyToolRegistered(
        tools,
        "toolWithFloatParam",
        "toolWithFloatParam",
        "toolWithFloatParam",
        Map.of("param", float.class));
    verifyToolRegistered(
        tools,
        "toolWithFloatClassParam",
        "toolWithFloatClassParam",
        "toolWithFloatClassParam",
        Map.of("param", Float.class));
    verifyToolRegistered(
        tools,
        "toolWithDoubleParam",
        "toolWithDoubleParam",
        "toolWithDoubleParam",
        Map.of("param", double.class));
    verifyToolRegistered(
        tools,
        "toolWithDoubleClassParam",
        "toolWithDoubleClassParam",
        "toolWithDoubleClassParam",
        Map.of("param", Double.class));
    verifyToolRegistered(
        tools,
        "toolWithNumberParam",
        "toolWithNumberParam",
        "toolWithNumberParam",
        Map.of("param", Number.class));
    verifyToolRegistered(
        tools,
        "toolWithBooleanParam",
        "toolWithBooleanParam",
        "toolWithBooleanParam",
        Map.of("param", boolean.class));
    verifyToolRegistered(
        tools,
        "toolWithBooleanClassParam",
        "toolWithBooleanClassParam",
        "toolWithBooleanClassParam",
        Map.of("param", Boolean.class));
    verifyToolRegistered(
        tools,
        "toolWithReturnStructuredContent",
        "toolWithReturnStructuredContent",
        "toolWithReturnStructuredContent",
        Map.of());
  }

  @SuppressWarnings("unchecked")
  private void verifyToolRegistered(
      List<McpSchema.Tool> tools,
      String toolName,
      String toolTitle,
      String toolDescription,
      Map<String, Class<?>> inputSchemaPropertiesTypes) {

    McpSchema.Tool tool =
        tools.stream().filter(t -> t.name().equals(toolName)).findAny().orElse(null);
    assertNotNull(tool);
    assertEquals(toolName, tool.name());
    assertEquals(toolTitle, tool.title());
    assertEquals(toolDescription, tool.description());
    assertEquals(inputSchemaPropertiesTypes.size(), tool.inputSchema().properties().size());

    // verify input schema properties types
    tool.inputSchema()
        .properties()
        .forEach(
            (name, property) -> {
              Map<String, String> props = (Map<String, String>) property;
              Class<?> javaClass = inputSchemaPropertiesTypes.get(name);
              final String jsonSchemaType = JavaTypeToJsonSchemaMapper.getJsonSchemaType(javaClass);
              assertEquals(jsonSchemaType, props.get("type"));
            });
  }

  private void verifyToolsCalled(McpSyncClient client) {
    verifyToolCalled(client, "toolWithDefaultName", Map.of(), "toolWithDefaultName is called");
    verifyToolCalled(client, "toolWithDefaultTitle", Map.of(), "toolWithDefaultTitle is called");
    verifyToolCalled(
        client, "toolWithDefaultDescription", Map.of(), "toolWithDefaultDescription is called");
    verifyToolCalled(client, "toolWithAllDefault", Map.of(), "toolWithAllDefault is called");
    verifyToolCalled(
        client,
        "toolWithOptionalParam",
        Map.of("param", "value"),
        "toolWithOptionalParam is called with optional param: value");
    verifyToolCalled(
        client,
        "toolWithRequiredParam",
        Map.of("param", "value"),
        "toolWithRequiredParam is called with required param: value");
    verifyToolCalled(
        client,
        "toolWithMultiParams",
        Map.of("param1", "value1", "param2", "value2"),
        "toolWithMultiParams is called with params: value1, value2");
    verifyToolCalled(
        client,
        "toolWithMixedParams",
        Map.of("mcpParam", "value"),
        "toolWithMixedParams is called with params: value, " + StringHelper.EMPTY);
    verifyToolCalled(
        client,
        "toolWithVoidReturn",
        Map.of(),
        "The method call succeeded but has a void return type");
    verifyToolCalled(
        client,
        "toolWithReturnNull",
        Map.of(),
        "The method call succeeded but the return value is null");
    verifyToolCalled(
        client,
        "toolWithIntParam",
        Map.of("param", 123),
        "toolWithIntParam is called with param: 123");
    verifyToolCalled(
        client,
        "toolWithIntegerParam",
        Map.of("param", 123),
        "toolWithIntegerParam is called with param: 123");
    verifyToolCalled(
        client,
        "toolWithLongParam",
        Map.of("param", 123L),
        "toolWithLongParam is called with param: 123");
    verifyToolCalled(
        client,
        "toolWithLongClassParam",
        Map.of("param", 123L),
        "toolWithLongClassParam is called with param: 123");
    verifyToolCalled(
        client,
        "toolWithFloatParam",
        Map.of("param", 123.0F),
        "toolWithFloatParam is called with param: 123.0");
    verifyToolCalled(
        client,
        "toolWithFloatClassParam",
        Map.of("param", 123.0F),
        "toolWithFloatClassParam is called with param: 123.0");
    verifyToolCalled(
        client,
        "toolWithDoubleParam",
        Map.of("param", 123.0),
        "toolWithDoubleParam is called with param: 123.0");
    verifyToolCalled(
        client,
        "toolWithDoubleClassParam",
        Map.of("param", 123.0),
        "toolWithDoubleClassParam is called with param: 123.0");
    verifyToolCalled(
        client,
        "toolWithNumberParam",
        Map.of("param", 123),
        "toolWithNumberParam is called with param: 123");
    verifyToolCalled(
        client,
        "toolWithBooleanParam",
        Map.of("param", true),
        "toolWithBooleanParam is called with param: true");
    verifyToolCalled(
        client,
        "toolWithBooleanClassParam",
        Map.of("param", true),
        "toolWithBooleanClassParam is called with param: true");
    verifyToolCalled(
        client,
        "toolWithReturnStructuredContent",
        Map.of(),
        new TestMcpToolsStructuredContent.TestStructuredContent(1, 2, 3L, 4L, 5.0F, 6.0F, 7.0, 8.0)
            .asTextContent());
  }

  private void verifyToolCalled(
      McpSyncClient client, String toolName, Map<String, Object> args, String expectedResult) {

    McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(toolName, args);
    McpSchema.CallToolResult result = client.callTool(request);
    McpSchema.TextContent content = (McpSchema.TextContent) result.content().get(0);
    assertFalse(result.isError());
    assertEquals(expectedResult, content.text());

    if (result.structuredContent() instanceof McpStructuredContent structuredContent) {
      assertEquals(expectedResult, structuredContent.asTextContent());
    }
  }
}
