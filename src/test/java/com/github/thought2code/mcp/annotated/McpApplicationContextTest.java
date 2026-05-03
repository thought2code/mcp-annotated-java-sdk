package com.github.thought2code.mcp.annotated;

import com.github.thought2code.mcp.annotated.annotation.McpTool;
import com.github.thought2code.mcp.context.alpha.AlphaMcpApplication;
import com.github.thought2code.mcp.context.beta.BetaMcpApplication;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McpApplicationContextTest {

  @Test
  void shouldKeepReflectionScopesIndependent() {
    McpApplicationContext alphaContext = McpApplicationContext.from(AlphaMcpApplication.class);
    McpApplicationContext betaContext = McpApplicationContext.from(BetaMcpApplication.class);

    assertEquals(Set.of("alphaTool"), toolNames(alphaContext));
    assertEquals(Set.of("betaTool"), toolNames(betaContext));
  }

  private Set<String> toolNames(McpApplicationContext context) {
    return context.getMethodsAnnotatedWith(McpTool.class).stream()
        .map(Method::getName)
        .collect(Collectors.toSet());
  }
}
