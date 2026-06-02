package com.github.thought2code.mcp.annotated;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.thought2code.mcp.annotated.annotation.McpServerApplication;
import com.github.thought2code.mcp.annotated.enums.McpServerError;
import com.github.thought2code.mcp.annotated.exception.McpServerException;
import com.github.thought2code.mcp.annotated.integration.IntegrationMcpApplication;
import com.github.thought2code.mcp.annotated.test.TestMcpTools;
import com.github.thought2code.mcp.context.alpha.AlphaMcpApplication;
import org.junit.jupiter.api.Test;

class McpApplicationContextTest {

  @Test
  void isInScope_shouldReturnFalseForBlankOrMalformedSourceMethod() {
    McpApplicationContext context = McpApplicationContext.from(IntegrationMcpApplication.class);

    assertFalse(context.isInScope(""));
    assertFalse(context.isInScope("   "));
    assertFalse(context.isInScope("NoHashMethod"));
  }

  @Test
  void isInScope_shouldFilterByResolvedBasePackage() {
    McpApplicationContext integrationContext =
        McpApplicationContext.from(IntegrationMcpApplication.class);

    assertTrue(
        integrationContext.isInScope(
            "com.github.thought2code.mcp.annotated.test.TestMcpTools#toolWithDefaultName()"));
    assertFalse(
        integrationContext.isInScope(
            "com.github.thought2code.mcp.context.alpha.AlphaTools#alphaTool()"));

    McpApplicationContext alphaContext = McpApplicationContext.from(AlphaMcpApplication.class);
    assertTrue(
        alphaContext.isInScope("com.github.thought2code.mcp.context.alpha.AlphaTools#alphaTool()"));
    assertFalse(
        alphaContext.isInScope(
            "com.github.thought2code.mcp.annotated.test.TestMcpTools#toolWithDefaultName()"));
  }

  @Test
  void from_shouldResolveBasePackageFromBasePackageAttribute() {
    McpApplicationContext context = McpApplicationContext.from(BetaBasePackageApplication.class);

    assertTrue(context.isInScope("com.github.thought2code.mcp.context.beta.BetaTools#betaTool()"));
    assertFalse(
        context.isInScope("com.github.thought2code.mcp.context.alpha.AlphaTools#alphaTool()"));
  }

  @Test
  void getComponentInstance_shouldCacheSingletonPerClass() {
    McpApplicationContext context = McpApplicationContext.from(IntegrationMcpApplication.class);

    Object first = context.getComponentInstance(TestMcpTools.class);
    Object second = context.getComponentInstance(TestMcpTools.class);

    assertSame(first, second);
  }

  @Test
  void getComponentInstance_shouldThrowWhenNoNoArgConstructor() {
    McpApplicationContext context = McpApplicationContext.from(IntegrationMcpApplication.class);

    McpServerException exception =
        assertThrows(
            McpServerException.class,
            () -> context.getComponentInstance(NoNoArgConstructorFixture.class));

    assertTrue(
        exception.getMessage().contains(McpServerError.COMPONENT_INSTANCE_CREATE_ERROR.getCode()));
    assertTrue(exception.getMessage().contains(NoNoArgConstructorFixture.class.getName()));
  }

  @McpServerApplication(basePackage = "com.github.thought2code.mcp.context.beta")
  static class BetaBasePackageApplication {}

  static class NoNoArgConstructorFixture {
    NoNoArgConstructorFixture(@SuppressWarnings("unused") String ignored) {}
  }
}
