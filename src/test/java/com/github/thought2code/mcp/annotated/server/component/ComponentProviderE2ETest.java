package com.github.thought2code.mcp.annotated.server.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.github.thought2code.mcp.annotated.McpApplication;
import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.integration.IntegrationMcpApplication;
import com.github.thought2code.mcp.annotated.server.component.completion.CompletionSupport;
import com.github.thought2code.mcp.annotated.server.component.prompt.PromptRegistration;
import com.github.thought2code.mcp.annotated.server.component.resource.ResourceRegistration;
import com.github.thought2code.mcp.annotated.server.component.tool.ToolRegistration;
import com.github.thought2code.mcp.context.alpha.AlphaMcpApplication;
import com.github.thought2code.mcp.context.beta.BetaMcpApplication;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ComponentProviderE2ETest {

  @Test
  void serviceLoader_shouldDiscoverGeneratedComponentProvidersFromAnnotationProcessorOutput() {
    List<ComponentProvider> providers = loadProviders();
    assertFalse(providers.isEmpty(), "Expected generated ComponentProvider implementations");

    boolean hasGeneratedProviderClass =
        providers.stream()
            .map(provider -> provider.getClass().getName())
            .anyMatch(
                className ->
                    className.startsWith(
                        "com.github.thought2code.mcp.annotated.generated.GeneratedComponentProvider_"));
    assertTrue(
        hasGeneratedProviderClass,
        "Expected at least one annotation-processor generated ComponentProvider");
  }

  @Test
  void toolRegistration_shouldRegisterGeneratedToolViaServiceLoader() {
    McpSyncServer server = mock(McpSyncServer.class);
    McpApplicationContext context = McpApplicationContext.from(IntegrationMcpApplication.class);

    boolean registered = ToolRegistration.registerSync(server, context);

    assertTrue(registered);
    verify(server, atLeastOnce()).addTool(any());
  }

  @Test
  void promptRegistration_shouldRegisterGeneratedPromptViaServiceLoader() {
    McpSyncServer server = mock(McpSyncServer.class);
    McpApplicationContext context = McpApplicationContext.from(IntegrationMcpApplication.class);

    boolean registered = PromptRegistration.registerSync(server, context);

    assertTrue(registered);
    verify(server, atLeastOnce()).addPrompt(any());
  }

  @Test
  void resourceRegistration_shouldRegisterGeneratedResourceViaServiceLoader() {
    McpSyncServer server = mock(McpSyncServer.class);
    McpApplicationContext context = McpApplicationContext.from(IntegrationMcpApplication.class);

    boolean registered = ResourceRegistration.registerSync(server, context);

    assertTrue(registered);
    verify(server, atLeastOnce()).addResource(any());
  }

  @Test
  void completionSupport_shouldBuildAndInvokeGeneratedCompletionViaServiceLoader() {
    McpApplicationContext context = McpApplicationContext.from(McpApplication.class);
    List<McpServerFeatures.SyncCompletionSpecification> completions =
        CompletionSupport.allSync(context);

    assertFalse(completions.isEmpty(), "Expected at least one generated completion");

    McpSchema.CompleteRequest request =
        new McpSchema.CompleteRequest(
            McpSchema.PromptReference.builder("generateCode").build(),
            new McpSchema.CompleteRequest.CompleteArgument("language", "J"));

    McpSchema.CompleteResult generateCodeResult = null;
    for (McpServerFeatures.SyncCompletionSpecification completion : completions) {
      McpSchema.CompleteResult result = completion.completionHandler().apply(null, request);
      if (List.of("Java", "Python").equals(result.completion().values())) {
        generateCodeResult = result;
        break;
      }
    }

    assertNotNull(generateCodeResult, "Expected generated completion values for 'generateCode'");
    assertEquals(2, generateCodeResult.completion().total());
    assertFalse(generateCodeResult.completion().hasMore());
  }

  @Test
  void alphaBasePackage_shouldOnlyRegisterAlphaToolFromGeneratedProviders() {
    McpSyncServer server = mock(McpSyncServer.class);
    McpApplicationContext alphaContext = McpApplicationContext.from(AlphaMcpApplication.class);

    boolean registered = ToolRegistration.registerSync(server, alphaContext);
    ArgumentCaptor<McpServerFeatures.SyncToolSpecification> captor =
        ArgumentCaptor.forClass(McpServerFeatures.SyncToolSpecification.class);

    assertTrue(registered);
    verify(server, atLeastOnce()).addTool(captor.capture());
    List<String> toolNames =
        captor.getAllValues().stream().map(spec -> spec.tool().name()).toList();
    assertEquals(List.of("alpha_tool"), toolNames);
    verify(server, atLeastOnce()).addTool(argThat(spec -> "alpha_tool".equals(spec.tool().name())));
    assertFalse(PromptRegistration.registerSync(mock(McpSyncServer.class), alphaContext));
    assertFalse(ResourceRegistration.registerSync(mock(McpSyncServer.class), alphaContext));
    assertTrue(CompletionSupport.allSync(alphaContext).isEmpty());
  }

  @Test
  void betaBasePackage_shouldOnlyRegisterBetaToolFromGeneratedProviders() {
    McpSyncServer server = mock(McpSyncServer.class);
    McpApplicationContext betaContext = McpApplicationContext.from(BetaMcpApplication.class);

    boolean registered = ToolRegistration.registerSync(server, betaContext);
    ArgumentCaptor<McpServerFeatures.SyncToolSpecification> captor =
        ArgumentCaptor.forClass(McpServerFeatures.SyncToolSpecification.class);

    assertTrue(registered);
    verify(server, atLeastOnce()).addTool(captor.capture());
    List<String> toolNames =
        captor.getAllValues().stream().map(spec -> spec.tool().name()).toList();
    assertEquals(List.of("beta_tool"), toolNames);
    verify(server, atLeastOnce()).addTool(argThat(spec -> "beta_tool".equals(spec.tool().name())));
    assertFalse(PromptRegistration.registerSync(mock(McpSyncServer.class), betaContext));
    assertFalse(ResourceRegistration.registerSync(mock(McpSyncServer.class), betaContext));
    assertTrue(CompletionSupport.allSync(betaContext).isEmpty());
  }

  private static List<ComponentProvider> loadProviders() {
    List<ComponentProvider> providers = new ArrayList<>();
    for (ComponentProvider provider : ServiceLoader.load(ComponentProvider.class)) {
      providers.add(provider);
    }
    return providers;
  }
}
