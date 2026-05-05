package com.github.thought2code.mcp.annotated.test;

import com.github.thought2code.mcp.annotated.annotation.McpPrompt;
import com.github.thought2code.mcp.annotated.annotation.McpPromptParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestMcpPrompts {

  private static final Logger log = LoggerFactory.getLogger(TestMcpPrompts.class);

  @McpPrompt(title = "title", description = "description")
  public String promptWithDefaultName() {
    log.debug("calling promptWithDefaultName");
    return "promptWithDefaultName is called";
  }

  @McpPrompt(name = "promptWithDefaultTitle", description = "description")
  public String promptWithDefaultTitle() {
    log.debug("calling promptWithDefaultTitle");
    return "promptWithDefaultTitle is called";
  }

  @McpPrompt(name = "promptWithDefaultDescription", title = "title")
  public String promptWithDefaultDescription() {
    log.debug("calling promptWithDefaultDescription");
    return "promptWithDefaultDescription is called";
  }

  @McpPrompt
  public String promptWithAllDefault() {
    log.debug("calling promptWithAllDefault");
    return "promptWithAllDefault is called";
  }

  @McpPrompt
  public String promptWithOptionalParam(
      @McpPromptParam(name = "param", description = "param") String param) {

    log.debug("calling promptWithOptionalParam with param: {}", param);
    return "promptWithOptionalParam is called with param: " + param;
  }

  @McpPrompt
  public String promptWithRequiredParam(
      @McpPromptParam(name = "param", description = "param", required = true) String param) {

    log.debug("calling promptWithRequiredParam with param: {}", param);
    return "promptWithRequiredParam is called with param: " + param;
  }

  @McpPrompt
  public String promptWithMultiParams(
      @McpPromptParam(name = "param1", description = "param1") String param1,
      @McpPromptParam(name = "param2", description = "param2", required = true) String param2) {

    log.debug("calling promptWithMultiParams with params: {}, {}", param1, param2);
    return String.format("promptWithMultiParams is called with params: %s, %s", param1, param2);
  }

  @McpPrompt
  public String promptWithMixedParams(
      @McpPromptParam(name = "mcpParam", description = "mcpParam") String mcpParam,
      String nonMcpParam) {

    log.debug("calling promptWithMixedParams with params: {}, {}", mcpParam, nonMcpParam);
    return String.format(
        "promptWithMixedParams is called with params: %s, %s", mcpParam, nonMcpParam);
  }

  @McpPrompt
  public String promptWithException() {
    throw new IllegalStateException("sensitive prompt failure detail");
  }
}
