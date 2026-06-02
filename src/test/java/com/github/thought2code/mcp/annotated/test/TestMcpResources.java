package com.github.thought2code.mcp.annotated.test;

import com.github.thought2code.mcp.annotated.annotation.McpResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestMcpResources {

  private static final Logger log = LoggerFactory.getLogger(TestMcpResources.class);

  @McpResource(
      uri = "test://resource1",
      name = "resource1_name",
      title = "resource1_title",
      description = "resource1_description")
  public String resource1() {
    log.debug("calling resource1");
    return "resource1_content";
  }

  @McpResource(
      uri = "file://{path}",
      name = "file_resource",
      title = "file_resource",
      description = "File resource for completion integration fixture")
  public String fileResource() {
    log.debug("calling fileResource");
    return "file_resource_content";
  }
}
