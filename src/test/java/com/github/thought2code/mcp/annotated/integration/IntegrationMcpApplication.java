package com.github.thought2code.mcp.annotated.integration;

import com.github.thought2code.mcp.annotated.annotation.McpServerApplication;
import com.github.thought2code.mcp.annotated.test.TestMcpTools;

/**
 * Bootstrap class for integration tests; scans all fixture components under {@code annotated.test}.
 */
@McpServerApplication(basePackageClass = TestMcpTools.class)
public class IntegrationMcpApplication {}
