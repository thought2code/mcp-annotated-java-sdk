package com.github.thought2code.mcp.annotated.integration;

import com.github.thought2code.mcp.annotated.annotation.McpServerApplication;
import com.github.thought2code.mcp.annotated.test.TestMcpTools;

/**
 * Bootstrap class for integration tests; registers fixture components under {@code annotated.test}
 * via base-package scope.
 */
@McpServerApplication(basePackageClass = TestMcpTools.class)
public class IntegrationMcpApplication {}
