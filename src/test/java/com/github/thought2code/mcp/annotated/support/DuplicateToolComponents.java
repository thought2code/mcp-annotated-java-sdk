package com.github.thought2code.mcp.annotated.support;

import com.github.thought2code.mcp.annotated.annotation.McpTool;

/** Fixture used to verify duplicate component name rejection during registration. */
public class DuplicateToolComponents {

  @McpTool(name = "duplicateTool")
  public String toolA() {
    return "a";
  }

  @McpTool(name = "duplicateTool")
  public String toolB() {
    return "b";
  }
}
