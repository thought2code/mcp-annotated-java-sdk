package com.github.thought2code.mcp.annotated.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.thought2code.mcp.annotated.enums.JavaTypeToJsonSchemaMapper;
import com.github.thought2code.mcp.annotated.enums.McpServerError;
import com.github.thought2code.mcp.annotated.server.McpStructuredContent;
import com.github.thought2code.mcp.annotated.test.TestMcpToolsStructuredContent;
import com.github.thought2code.mcp.annotated.util.StringHelper;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.Map;

/** Shared MCP client assertions used by CRL integration tests. */
public final class McpClientVerificationSupport {

  private McpClientVerificationSupport() {}

  public static void verifyAll(McpSyncClient client) {
    verifyServerInfo(client);
    verifyResourcesRegistered(client);
    verifyResourcesCalled(client);
    verifyPromptsRegistered(client);
    verifyPromptsCalled(client);
    verifyToolsRegistered(client);
    verifyToolsCalled(client);
    verifyCompletions(client);
  }

  public static void verifyCompletions(McpSyncClient client) {
    McpSchema.CompleteRequest promptRequest =
        McpSchema.CompleteRequest.builder(
                McpSchema.PromptReference.builder("generateCode").build(),
                new McpSchema.CompleteRequest.CompleteArgument("language", ""))
            .build();
    McpSchema.CompleteResult promptResult = client.completeCompletion(promptRequest);
    assertEquals(List.of("Java", "Python"), promptResult.completion().values());
    assertEquals(2, promptResult.completion().total());
    assertFalse(promptResult.completion().hasMore());

    McpSchema.CompleteRequest resourceRequest =
        McpSchema.CompleteRequest.builder(
                new McpSchema.ResourceReference("file://{path}"),
                new McpSchema.CompleteRequest.CompleteArgument("path", "file"))
            .build();
    McpSchema.CompleteResult resourceResult = client.completeCompletion(resourceRequest);
    assertEquals(List.of("file://a", "file://b"), resourceResult.completion().values());
    assertEquals(2, resourceResult.completion().total());
    assertTrue(resourceResult.completion().hasMore());
  }

  public static void verifyServerInfo(McpSyncClient client) {
    McpSchema.InitializeResult initialized = client.initialize();
    assertEquals("mcp-server", initialized.serverInfo().name());
    assertEquals("1.0.0", initialized.serverInfo().version());
    assertEquals("test", initialized.instructions());
  }

  public static void verifyResourcesRegistered(McpSyncClient client) {
    List<McpSchema.Resource> resources = client.listResources().resources();
    assertEquals(3, resources.size());
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
    verifyResourceRegistered(
        resources,
        "file://{path}",
        "file_resource",
        "file_resource",
        "File resource for completion integration fixture");
  }

  private static void verifyResourceRegistered(
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

  public static void verifyResourcesCalled(McpSyncClient client) {
    verifyResourceCalled(client, "test://resource1", "text/plain", "resource1_content");
    verifyResourceCalled(client, "test://resource2", "text/plain", "resource2_content");
  }

  private static void verifyResourceCalled(
      McpSyncClient client, String resourceUri, String resourceMimeType, String resourceContent) {
    McpSchema.ReadResourceRequest request =
        McpSchema.ReadResourceRequest.builder(resourceUri).build();
    McpSchema.ReadResourceResult result = client.readResource(request);
    McpSchema.TextResourceContents content =
        (McpSchema.TextResourceContents) result.contents().get(0);
    assertNotNull(content);
    assertEquals(resourceUri, content.uri());
    assertEquals(resourceMimeType, content.mimeType());
    assertEquals(resourceContent, content.text());
  }

  public static void verifyPromptsRegistered(McpSyncClient client) {
    List<McpSchema.Prompt> prompts = client.listPrompts().prompts();
    assertEquals(12, prompts.size());
    verifyPromptRegistered(prompts, "prompt_with_default_name", "title", "description", 0);
    verifyPromptRegistered(
        prompts, "promptWithDefaultTitle", "promptWithDefaultTitle", "description", 0);
    verifyPromptRegistered(
        prompts, "promptWithDefaultDescription", "title", "promptWithDefaultDescription", 0);
    verifyPromptRegistered(
        prompts,
        "prompt_with_all_default",
        "prompt_with_all_default",
        "prompt_with_all_default",
        0);
    verifyPromptRegistered(
        prompts,
        "prompt_with_optional_param",
        "prompt_with_optional_param",
        "prompt_with_optional_param",
        1);
    verifyPromptRegistered(
        prompts,
        "prompt_with_required_param",
        "prompt_with_required_param",
        "prompt_with_required_param",
        1);
    verifyPromptRegistered(
        prompts,
        "prompt_with_multi_params",
        "prompt_with_multi_params",
        "prompt_with_multi_params",
        2);
    verifyPromptRegistered(
        prompts,
        "prompt_with_mixed_params",
        "prompt_with_mixed_params",
        "prompt_with_mixed_params",
        1);
    verifyPromptRegistered(
        prompts,
        "prompt_with_void_return",
        "prompt_with_void_return",
        "prompt_with_void_return",
        0);
    verifyPromptRegistered(
        prompts,
        "prompt_with_return_null",
        "prompt_with_return_null",
        "prompt_with_return_null",
        0);
    verifyPromptRegistered(
        prompts,
        "generateCode",
        "generateCode",
        "Prompt used by completion integration fixture",
        1);
    verifyPromptRegistered(
        prompts, "prompt_with_exception", "prompt_with_exception", "prompt_with_exception", 0);
  }

  private static void verifyPromptRegistered(
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

  public static void verifyPromptsCalled(McpSyncClient client) {
    verifyPromptCalled(
        client, "prompt_with_default_name", Map.of(), "promptWithDefaultName is called");
    verifyPromptCalled(
        client, "promptWithDefaultTitle", Map.of(), "promptWithDefaultTitle is called");
    verifyPromptCalled(
        client, "promptWithDefaultDescription", Map.of(), "promptWithDefaultDescription is called");
    verifyPromptCalled(
        client, "prompt_with_all_default", Map.of(), "promptWithAllDefault is called");
    verifyPromptCalled(
        client,
        "prompt_with_optional_param",
        Map.of("param", "value"),
        "promptWithOptionalParam is called with param: value");
    verifyPromptCalled(
        client,
        "prompt_with_required_param",
        Map.of("param", "value"),
        "promptWithRequiredParam is called with param: value");
    verifyPromptCalled(
        client,
        "prompt_with_multi_params",
        Map.of("param1", "value1", "param2", "value2"),
        "promptWithMultiParams is called with params: value1, value2");
    verifyPromptCalled(
        client,
        "prompt_with_mixed_params",
        Map.of("mcpParam", "value"),
        "promptWithMixedParams is called with params: value, " + StringHelper.EMPTY);
    verifyPromptCalled(
        client,
        "prompt_with_void_return",
        Map.of(),
        "The method call succeeded but has a void return type");
    verifyPromptCalled(
        client,
        "prompt_with_return_null",
        Map.of(),
        "The method call succeeded but the return value is null");
    verifyPromptCalled(
        client,
        "generateCode",
        Map.of("language", "Java"),
        "generateCode is called with language: Java");
    verifyPromptCalled(
        client,
        "prompt_with_exception",
        Map.of(),
        McpServerError.METHOD_INVOCATION_ERROR.toString());
  }

  private static void verifyPromptCalled(
      McpSyncClient client, String promptName, Map<String, Object> params, String expectedResult) {
    McpSchema.GetPromptRequest request =
        McpSchema.GetPromptRequest.builder(promptName).arguments(params).build();
    McpSchema.GetPromptResult result = client.getPrompt(request);
    McpSchema.TextContent content = (McpSchema.TextContent) result.messages().get(0).content();
    assertEquals(expectedResult, content.text());
  }

  public static void verifyToolsRegistered(McpSyncClient client) {
    List<McpSchema.Tool> tools = client.listTools().tools();
    assertEquals(23, tools.size());
    verifyToolRegistered(tools, "tool_with_default_name", "title", "description", Map.of());
    verifyToolRegistered(
        tools, "toolWithDefaultTitle", "toolWithDefaultTitle", "description", Map.of());
    verifyToolRegistered(
        tools, "toolWithDefaultDescription", "title", "toolWithDefaultDescription", Map.of());
    verifyToolRegistered(
        tools, "tool_with_all_default", "tool_with_all_default", "tool_with_all_default", Map.of());
    verifyToolRegistered(
        tools,
        "tool_with_optional_param",
        "tool_with_optional_param",
        "tool_with_optional_param",
        Map.of("param", String.class));
    verifyToolRegistered(
        tools,
        "tool_with_required_param",
        "tool_with_required_param",
        "tool_with_required_param",
        Map.of("param", String.class));
    verifyToolRegistered(
        tools,
        "tool_with_multi_params",
        "tool_with_multi_params",
        "tool_with_multi_params",
        Map.of("param1", String.class, "param2", String.class));
    verifyToolRegistered(
        tools,
        "tool_with_mixed_params",
        "tool_with_mixed_params",
        "tool_with_mixed_params",
        Map.of("mcpParam", String.class));
    verifyToolRegistered(
        tools, "tool_with_void_return", "tool_with_void_return", "tool_with_void_return", Map.of());
    verifyToolRegistered(
        tools, "tool_with_return_null", "tool_with_return_null", "tool_with_return_null", Map.of());
    verifyToolRegistered(
        tools,
        "tool_with_int_param",
        "tool_with_int_param",
        "tool_with_int_param",
        Map.of("param", int.class));
    verifyToolRegistered(
        tools,
        "tool_with_integer_param",
        "tool_with_integer_param",
        "tool_with_integer_param",
        Map.of("param", Integer.class));
    verifyToolRegistered(
        tools,
        "tool_with_long_param",
        "tool_with_long_param",
        "tool_with_long_param",
        Map.of("param", long.class));
    verifyToolRegistered(
        tools,
        "tool_with_long_class_param",
        "tool_with_long_class_param",
        "tool_with_long_class_param",
        Map.of("param", Long.class));
    verifyToolRegistered(
        tools,
        "tool_with_float_param",
        "tool_with_float_param",
        "tool_with_float_param",
        Map.of("param", float.class));
    verifyToolRegistered(
        tools,
        "tool_with_float_class_param",
        "tool_with_float_class_param",
        "tool_with_float_class_param",
        Map.of("param", Float.class));
    verifyToolRegistered(
        tools,
        "tool_with_double_param",
        "tool_with_double_param",
        "tool_with_double_param",
        Map.of("param", double.class));
    verifyToolRegistered(
        tools,
        "tool_with_double_class_param",
        "tool_with_double_class_param",
        "tool_with_double_class_param",
        Map.of("param", Double.class));
    verifyToolRegistered(
        tools,
        "tool_with_number_param",
        "tool_with_number_param",
        "tool_with_number_param",
        Map.of("param", Number.class));
    verifyToolRegistered(
        tools,
        "tool_with_boolean_param",
        "tool_with_boolean_param",
        "tool_with_boolean_param",
        Map.of("param", boolean.class));
    verifyToolRegistered(
        tools,
        "tool_with_boolean_class_param",
        "tool_with_boolean_class_param",
        "tool_with_boolean_class_param",
        Map.of("param", Boolean.class));
    verifyToolRegistered(
        tools,
        "tool_with_return_structured_content",
        "tool_with_return_structured_content",
        "tool_with_return_structured_content",
        Map.of());
    verifyToolRegistered(
        tools, "tool_with_exception", "tool_with_exception", "tool_with_exception", Map.of());
  }

  @SuppressWarnings("unchecked")
  private static void verifyToolRegistered(
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
    Map<String, Object> inputSchema = tool.inputSchema();
    assertNotNull(inputSchema);
    Map<String, Object> properties = (Map<String, Object>) inputSchema.get("properties");
    assertNotNull(properties);
    assertEquals(inputSchemaPropertiesTypes.size(), properties.size());
    properties.forEach(
        (name, property) -> {
          Map<String, String> props = (Map<String, String>) property;
          Class<?> javaClass = inputSchemaPropertiesTypes.get(name);
          assertEquals(JavaTypeToJsonSchemaMapper.getJsonSchemaType(javaClass), props.get("type"));
        });
  }

  public static void verifyToolsCalled(McpSyncClient client) {
    verifyToolCalled(client, "tool_with_default_name", Map.of(), "toolWithDefaultName is called");
    verifyToolCalled(client, "toolWithDefaultTitle", Map.of(), "toolWithDefaultTitle is called");
    verifyToolCalled(
        client, "toolWithDefaultDescription", Map.of(), "toolWithDefaultDescription is called");
    verifyToolCalled(client, "tool_with_all_default", Map.of(), "toolWithAllDefault is called");
    verifyToolCalled(
        client,
        "tool_with_optional_param",
        Map.of("param", "value"),
        "toolWithOptionalParam is called with optional param: value");
    verifyToolCalled(
        client,
        "tool_with_required_param",
        Map.of("param", "value"),
        "toolWithRequiredParam is called with required param: value");
    verifyToolCalled(
        client,
        "tool_with_multi_params",
        Map.of("param1", "value1", "param2", "value2"),
        "toolWithMultiParams is called with params: value1, value2");
    verifyToolCalled(
        client,
        "tool_with_mixed_params",
        Map.of("mcpParam", "value"),
        "toolWithMixedParams is called with params: value, " + StringHelper.EMPTY);
    verifyToolCalled(
        client,
        "tool_with_void_return",
        Map.of(),
        "The method call succeeded but has a void return type");
    verifyToolCalled(
        client,
        "tool_with_return_null",
        Map.of(),
        "The method call succeeded but the return value is null");
    verifyToolCalled(
        client,
        "tool_with_int_param",
        Map.of("param", 123),
        "toolWithIntParam is called with param: 123");
    verifyToolCalled(
        client,
        "tool_with_integer_param",
        Map.of("param", 123),
        "toolWithIntegerParam is called with param: 123");
    verifyToolCalled(
        client,
        "tool_with_long_param",
        Map.of("param", 123L),
        "toolWithLongParam is called with param: 123");
    verifyToolCalled(
        client,
        "tool_with_long_class_param",
        Map.of("param", 123L),
        "toolWithLongClassParam is called with param: 123");
    verifyToolCalled(
        client,
        "tool_with_float_param",
        Map.of("param", 123.0F),
        "toolWithFloatParam is called with param: 123.0");
    verifyToolCalled(
        client,
        "tool_with_float_class_param",
        Map.of("param", 123.0F),
        "toolWithFloatClassParam is called with param: 123.0");
    verifyToolCalled(
        client,
        "tool_with_double_param",
        Map.of("param", 123.0),
        "toolWithDoubleParam is called with param: 123.0");
    verifyToolCalled(
        client,
        "tool_with_double_class_param",
        Map.of("param", 123.0),
        "toolWithDoubleClassParam is called with param: 123.0");
    verifyToolCalled(
        client,
        "tool_with_number_param",
        Map.of("param", 123),
        "toolWithNumberParam is called with param: 123");
    verifyToolCalled(
        client,
        "tool_with_boolean_param",
        Map.of("param", true),
        "toolWithBooleanParam is called with param: true");
    verifyToolCalled(
        client,
        "tool_with_boolean_class_param",
        Map.of("param", true),
        "toolWithBooleanClassParam is called with param: true");
    verifyToolCalled(
        client,
        "tool_with_return_structured_content",
        Map.of(),
        new TestMcpToolsStructuredContent.TestStructuredContent(1, 2, 3L, 4L, 5.0F, 6.0F, 7.0, 8.0)
            .asTextContent());
    verifyToolCalledError(
        client, "tool_with_exception", Map.of(), McpServerError.METHOD_INVOCATION_ERROR.toString());
  }

  private static void verifyToolCalled(
      McpSyncClient client, String toolName, Map<String, Object> args, String expectedResult) {
    McpSchema.CallToolRequest request =
        McpSchema.CallToolRequest.builder(toolName).arguments(args).build();
    McpSchema.CallToolResult result = client.callTool(request);
    McpSchema.TextContent content = (McpSchema.TextContent) result.content().get(0);
    assertFalse(result.isError());
    assertEquals(expectedResult, content.text());
    if (result.structuredContent() instanceof McpStructuredContent structuredContent) {
      assertEquals(expectedResult, structuredContent.asTextContent());
    }
  }

  private static void verifyToolCalledError(
      McpSyncClient client, String toolName, Map<String, Object> args, String expectedResult) {
    McpSchema.CallToolRequest request =
        McpSchema.CallToolRequest.builder(toolName).arguments(args).build();
    McpSchema.CallToolResult result = client.callTool(request);
    McpSchema.TextContent content = (McpSchema.TextContent) result.content().get(0);
    assertTrue(result.isError());
    assertEquals(expectedResult, content.text());
  }
}
