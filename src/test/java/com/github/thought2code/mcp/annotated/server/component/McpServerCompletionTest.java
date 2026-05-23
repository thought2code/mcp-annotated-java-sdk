package com.github.thought2code.mcp.annotated.server.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.annotation.McpPromptCompletion;
import com.github.thought2code.mcp.annotated.annotation.McpResourceCompletion;
import com.github.thought2code.mcp.annotated.exception.McpServerComponentRegistrationException;
import com.github.thought2code.mcp.annotated.support.InvalidMcpCompletions;
import com.github.thought2code.mcp.annotated.support.TestMcpCompletions;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class McpServerCompletionTest {

  @Test
  void allSync_shouldCreateSpecificationsForPromptAndResourceCompletions() throws Exception {
    Method promptMethod =
        TestMcpCompletions.class.getDeclaredMethod(
            "completeGenerateCode", McpSchema.CompleteRequest.CompleteArgument.class);
    Method resourceMethod =
        TestMcpCompletions.class.getDeclaredMethod(
            "completeFileUri", McpSchema.CompleteRequest.CompleteArgument.class);

    McpApplicationContext context = mock(McpApplicationContext.class);
    when(context.getMethodsAnnotatedWith(McpPromptCompletion.class))
        .thenReturn(Set.of(promptMethod));
    when(context.getMethodsAnnotatedWith(McpResourceCompletion.class))
        .thenReturn(Set.of(resourceMethod));
    when(context.getComponentInstance(TestMcpCompletions.class))
        .thenReturn(new TestMcpCompletions());

    List<McpServerFeatures.SyncCompletionSpecification> completions =
        McpServerCompletion.allSync(context);

    assertEquals(2, completions.size());

    McpServerFeatures.SyncCompletionSpecification promptSpec =
        completions.stream()
            .filter(spec -> spec.referenceKey() instanceof McpSchema.PromptReference)
            .findFirst()
            .orElseThrow();
    McpSchema.PromptReference promptRef = (McpSchema.PromptReference) promptSpec.referenceKey();
    assertEquals("generateCode", promptRef.name());
    assertEquals("Code languages", promptRef.title());

    McpServerFeatures.SyncCompletionSpecification resourceSpec =
        completions.stream()
            .filter(spec -> spec.referenceKey() instanceof McpSchema.ResourceReference)
            .findFirst()
            .orElseThrow();
    assertEquals("file://", ((McpSchema.ResourceReference) resourceSpec.referenceKey()).uri());
  }

  @Test
  void allAsync_shouldCreateSpecificationsForPromptAndResourceCompletions() throws Exception {
    Method promptMethod =
        TestMcpCompletions.class.getDeclaredMethod(
            "completeGenerateCode", McpSchema.CompleteRequest.CompleteArgument.class);
    Method resourceMethod =
        TestMcpCompletions.class.getDeclaredMethod(
            "completeFileUri", McpSchema.CompleteRequest.CompleteArgument.class);

    McpApplicationContext context = mock(McpApplicationContext.class);
    when(context.getMethodsAnnotatedWith(McpPromptCompletion.class))
        .thenReturn(Set.of(promptMethod));
    when(context.getMethodsAnnotatedWith(McpResourceCompletion.class))
        .thenReturn(Set.of(resourceMethod));
    when(context.getComponentInstance(TestMcpCompletions.class))
        .thenReturn(new TestMcpCompletions());

    List<McpServerFeatures.AsyncCompletionSpecification> completions =
        McpServerCompletion.allAsync(context);

    assertEquals(2, completions.size());
    assertTrue(
        completions.stream()
            .anyMatch(spec -> spec.referenceKey() instanceof McpSchema.PromptReference));
    assertTrue(
        completions.stream()
            .anyMatch(spec -> spec.referenceKey() instanceof McpSchema.ResourceReference));
  }

  @Test
  void allSync_shouldRejectInvalidReturnType() throws Exception {
    Method method =
        InvalidMcpCompletions.class.getDeclaredMethod(
            "badReturnType", McpSchema.CompleteRequest.CompleteArgument.class);

    McpApplicationContext context = mock(McpApplicationContext.class);
    when(context.getMethodsAnnotatedWith(McpPromptCompletion.class)).thenReturn(Set.of(method));
    when(context.getMethodsAnnotatedWith(McpResourceCompletion.class)).thenReturn(Set.of());

    McpServerComponentRegistrationException exception =
        assertThrows(
            McpServerComponentRegistrationException.class,
            () -> McpServerCompletion.allSync(context));

    assertTrue(
        exception.getMessage().contains("Completion method must return McpCompleteCompletion"));
  }

  @Test
  void allSync_shouldRejectInvalidParameterType() throws Exception {
    Method method = InvalidMcpCompletions.class.getDeclaredMethod("badParameterType", String.class);

    McpApplicationContext context = mock(McpApplicationContext.class);
    when(context.getMethodsAnnotatedWith(McpPromptCompletion.class)).thenReturn(Set.of(method));
    when(context.getMethodsAnnotatedWith(McpResourceCompletion.class)).thenReturn(Set.of());

    McpServerComponentRegistrationException exception =
        assertThrows(
            McpServerComponentRegistrationException.class,
            () -> McpServerCompletion.allSync(context));

    assertTrue(
        exception
            .getMessage()
            .contains(
                "Completion method must have exactly one parameter of type McpSchema.CompleteRequest.CompleteArgument"));
  }

  @Test
  void allAsync_shouldRejectInvalidReturnType() throws Exception {
    Method method =
        InvalidMcpCompletions.class.getDeclaredMethod(
            "badReturnType", McpSchema.CompleteRequest.CompleteArgument.class);

    McpApplicationContext context = mock(McpApplicationContext.class);
    when(context.getMethodsAnnotatedWith(McpPromptCompletion.class)).thenReturn(Set.of(method));
    when(context.getMethodsAnnotatedWith(McpResourceCompletion.class)).thenReturn(Set.of());

    McpServerComponentRegistrationException exception =
        assertThrows(
            McpServerComponentRegistrationException.class,
            () -> McpServerCompletion.allAsync(context));

    assertTrue(
        exception.getMessage().contains("Completion method must return McpCompleteCompletion"));
  }

  @Test
  void allAsync_shouldRejectInvalidParameterType() throws Exception {
    Method method = InvalidMcpCompletions.class.getDeclaredMethod("badParameterType", String.class);

    McpApplicationContext context = mock(McpApplicationContext.class);
    when(context.getMethodsAnnotatedWith(McpPromptCompletion.class)).thenReturn(Set.of(method));
    when(context.getMethodsAnnotatedWith(McpResourceCompletion.class)).thenReturn(Set.of());

    McpServerComponentRegistrationException exception =
        assertThrows(
            McpServerComponentRegistrationException.class,
            () -> McpServerCompletion.allAsync(context));

    assertTrue(
        exception
            .getMessage()
            .contains(
                "Completion method must have exactly one parameter of type McpSchema.CompleteRequest.CompleteArgument"));
  }

  @Test
  void syncHandler_shouldInvokePromptCompletionAndMapResult() throws Exception {
    Method method =
        TestMcpCompletions.class.getDeclaredMethod(
            "completeGenerateCode", McpSchema.CompleteRequest.CompleteArgument.class);

    McpApplicationContext context = mock(McpApplicationContext.class);
    when(context.getMethodsAnnotatedWith(McpPromptCompletion.class)).thenReturn(Set.of(method));
    when(context.getMethodsAnnotatedWith(McpResourceCompletion.class)).thenReturn(Set.of());
    when(context.getComponentInstance(TestMcpCompletions.class))
        .thenReturn(new TestMcpCompletions());

    McpServerFeatures.SyncCompletionSpecification specification =
        McpServerCompletion.allSync(context).get(0);

    McpSchema.CompleteReference ref =
        McpSchema.PromptReference.builder("generateCode").title("Code languages").build();
    McpSchema.CompleteRequest request =
        new McpSchema.CompleteRequest(
            ref, new McpSchema.CompleteRequest.CompleteArgument("language", "Jav"));

    McpSchema.CompleteResult result = specification.completionHandler().apply(null, request);

    assertEquals(List.of("Java", "Python"), result.completion().values());
    assertEquals(2, result.completion().total());
    assertEquals(false, result.completion().hasMore());
    assertInstanceOf(McpSchema.PromptReference.class, specification.referenceKey());
  }

  @Test
  void syncHandler_shouldInvokeResourceCompletionAndMapResult() throws Exception {
    Method method =
        TestMcpCompletions.class.getDeclaredMethod(
            "completeFileUri", McpSchema.CompleteRequest.CompleteArgument.class);

    McpApplicationContext context = mock(McpApplicationContext.class);
    when(context.getMethodsAnnotatedWith(McpPromptCompletion.class)).thenReturn(Set.of());
    when(context.getMethodsAnnotatedWith(McpResourceCompletion.class)).thenReturn(Set.of(method));
    when(context.getComponentInstance(TestMcpCompletions.class))
        .thenReturn(new TestMcpCompletions());

    McpServerFeatures.SyncCompletionSpecification specification =
        McpServerCompletion.allSync(context).get(0);

    McpSchema.CompleteReference ref = new McpSchema.ResourceReference("file://");
    McpSchema.CompleteRequest request =
        new McpSchema.CompleteRequest(
            ref, new McpSchema.CompleteRequest.CompleteArgument("uri", "file://a"));

    McpSchema.CompleteResult result = specification.completionHandler().apply(null, request);

    assertEquals(List.of("file://a", "file://b"), result.completion().values());
    assertEquals(2, result.completion().total());
    assertEquals(true, result.completion().hasMore());
    assertInstanceOf(McpSchema.ResourceReference.class, specification.referenceKey());
  }

  @Test
  void asyncHandler_shouldInvokeCompletionAndReturnMono() throws Exception {
    Method method =
        TestMcpCompletions.class.getDeclaredMethod(
            "completeGenerateCode", McpSchema.CompleteRequest.CompleteArgument.class);

    McpApplicationContext context = mock(McpApplicationContext.class);
    when(context.getMethodsAnnotatedWith(McpPromptCompletion.class)).thenReturn(Set.of(method));
    when(context.getMethodsAnnotatedWith(McpResourceCompletion.class)).thenReturn(Set.of());
    when(context.getComponentInstance(TestMcpCompletions.class))
        .thenReturn(new TestMcpCompletions());

    McpServerFeatures.AsyncCompletionSpecification specification =
        McpServerCompletion.allAsync(context).get(0);

    McpSchema.CompleteReference ref = McpSchema.PromptReference.builder("generateCode").build();
    McpSchema.CompleteRequest request =
        new McpSchema.CompleteRequest(
            ref, new McpSchema.CompleteRequest.CompleteArgument("language", "Jav"));

    McpSchema.CompleteResult result =
        specification.completionHandler().apply(null, request).block();

    assertEquals(List.of("Java", "Python"), result.completion().values());
    assertEquals(2, result.completion().total());
    assertEquals(false, result.completion().hasMore());
  }
}
