package com.github.thought2code.mcp.annotated.test;

import com.github.thought2code.mcp.annotated.annotation.McpTool;
import com.github.thought2code.mcp.annotated.annotation.McpToolParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestMcpTools {

  private static final Logger log = LoggerFactory.getLogger(TestMcpTools.class);

  @McpTool(title = "title", description = "description")
  public String toolWithDefaultName() {
    log.debug("calling toolWithDefaultName");
    return "toolWithDefaultName is called";
  }

  @McpTool(name = "toolWithDefaultTitle", description = "description")
  public String toolWithDefaultTitle() {
    log.debug("calling toolWithDefaultTitle");
    return "toolWithDefaultTitle is called";
  }

  @McpTool(name = "toolWithDefaultDescription", title = "title")
  public String toolWithDefaultDescription() {
    log.debug("calling toolWithDefaultDescription");
    return "toolWithDefaultDescription is called";
  }

  @McpTool
  public String toolWithAllDefault() {
    log.debug("calling toolWithAllDefault");
    return "toolWithAllDefault is called";
  }

  @McpTool
  public String toolWithOptionalParam(
      @McpToolParam(name = "param", description = "param") String param) {

    log.debug("calling toolWithOptionalParam with param: {}", param);
    return "toolWithOptionalParam is called with optional param: " + param;
  }

  @McpTool
  public String toolWithRequiredParam(
      @McpToolParam(name = "param", description = "param", required = true) String param) {

    log.debug("calling toolWithRequiredParam with param: {}", param);
    return "toolWithRequiredParam is called with required param: " + param;
  }

  @McpTool
  public String toolWithMultiParams(
      @McpToolParam(name = "param1", description = "param1") String param1,
      @McpToolParam(name = "param2", description = "param2", required = true) String param2) {

    log.debug("calling toolWithMultiParams with params: {}, {}", param1, param2);
    return String.format("toolWithMultiParams is called with params: %s, %s", param1, param2);
  }

  @McpTool
  public String toolWithMixedParams(
      @McpToolParam(name = "mcpParam", description = "mcpParam") String mcpParam,
      String nonMcpParam) {

    log.debug("calling toolWithMixedParams with params: {}, {}", mcpParam, nonMcpParam);
    return String.format(
        "toolWithMixedParams is called with params: %s, %s", mcpParam, nonMcpParam);
  }

  @McpTool
  public String toolWithException() {
    throw new IllegalStateException("sensitive tool failure detail");
  }
}
