package com.github.thought2code.mcp.annotated.server.component;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.exception.McpServerComponentRegistrationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import reactor.core.publisher.Mono;

class ComponentRegistrationSupportTest {

  @Test
  void loadDefinitions_shouldFilterOutOfScopeDefinitions() {
    ComponentProvider providerA = new ComponentProvider() {};
    ComponentProvider providerB = new ComponentProvider() {};
    Def inScope = new Def("a", "in.scope.A#x()");
    Def outOfScope = new Def("b", "out.scope.B#y()");
    Map<ComponentProvider, List<Def>> definitionsByProvider = new HashMap<>();
    definitionsByProvider.put(providerA, List.of(inScope));
    definitionsByProvider.put(providerB, List.of(outOfScope));

    McpApplicationContext context = mock(McpApplicationContext.class);
    when(context.isInScope("in.scope.A#x()")).thenReturn(true);
    when(context.isInScope("out.scope.B#y()")).thenReturn(false);

    List<Def> definitions =
        ComponentRegistrationSupport.loadDefinitions(
            List.of(providerA, providerB), context, definitionsByProvider::get, Def::sourceMethod);

    assertEquals(List.of(inScope), definitions);
  }

  @Test
  void prepareDefinitions_shouldLoadAndRejectDuplicatesInOneStep() {
    ComponentProvider provider = new ComponentProvider() {};
    Def first = new Def("dup", "in.scope.A#a()");
    Def second = new Def("dup", "in.scope.B#b()");
    Map<ComponentProvider, List<Def>> definitionsByProvider = new HashMap<>();
    definitionsByProvider.put(provider, List.of(first, second));

    McpApplicationContext context = mock(McpApplicationContext.class);
    when(context.isInScope("in.scope.A#a()")).thenReturn(true);
    when(context.isInScope("in.scope.B#b()")).thenReturn(true);

    McpServerComponentRegistrationException exception =
        assertThrows(
            McpServerComponentRegistrationException.class,
            () ->
                ComponentRegistrationSupport.prepareDefinitions(
                    List.of(provider),
                    context,
                    definitionsByProvider::get,
                    Def::sourceMethod,
                    Def::name,
                    DuplicateComponentMessageHelper::duplicateToolName));
    assertEquals(
        "Duplicate tool name 'dup' found for methods in.scope.A#a() and in.scope.B#b()",
        exception.getMessage());
  }

  @Test
  void rejectDuplicateNames_shouldThrowWhenDuplicateExists() {
    Def first = new Def("duplicate", "test.Source#a()");
    Def second = new Def("duplicate", "test.Source#b()");

    McpServerComponentRegistrationException exception =
        assertThrows(
            McpServerComponentRegistrationException.class,
            () ->
                ComponentRegistrationSupport.rejectDuplicateNames(
                    List.of(first, second),
                    Def::name,
                    Def::sourceMethod,
                    DuplicateComponentMessageHelper::duplicateToolName));
    assertEquals(
        "Duplicate tool name 'duplicate' found for methods test.Source#a() and test.Source#b()",
        exception.getMessage());
  }

  @Test
  void rejectDuplicateDefinitions_shouldUseCustomMessageBuilder() {
    Def first = new Def("dup", "test.Source#a()");
    Def second = new Def("dup", "test.Source#b()");

    McpServerComponentRegistrationException exception =
        assertThrows(
            McpServerComponentRegistrationException.class,
            () ->
                ComponentRegistrationSupport.rejectDuplicateDefinitions(
                    List.of(first, second),
                    Def::name,
                    Def::sourceMethod,
                    (definition, previous, current) ->
                        "custom:" + definition.name() + ":" + previous + ":" + current));
    assertEquals("custom:dup:test.Source#a():test.Source#b()", exception.getMessage());
  }

  @Test
  void awaitAsyncRegistration_shouldWrapFailureWithStandardMessage() {
    Logger logger = mock(Logger.class);
    RuntimeException cause = new RuntimeException("boom");

    McpServerComponentRegistrationException exception =
        assertThrows(
            McpServerComponentRegistrationException.class,
            () ->
                ComponentRegistrationSupport.awaitAsyncRegistration(
                    Mono.error(cause), "McpTool", "tool_name", logger));
    assertEquals("Failed to register async McpTool tool_name", exception.getMessage());
  }

  @Test
  void awaitAsyncRegistration_shouldPassOnSuccess() {
    Logger logger = mock(Logger.class);
    assertDoesNotThrow(
        () ->
            ComponentRegistrationSupport.awaitAsyncRegistration(
                Mono.empty(), "McpPrompt", "prompt_name", logger));
  }

  @Test
  void registerSyncDefinitions_shouldReturnFalseWhenNoDefinition() {
    AtomicInteger counter = new AtomicInteger();
    boolean registered =
        ComponentRegistrationSupport.registerSyncDefinitions(
            List.of(), def -> counter.incrementAndGet(), def -> counter.incrementAndGet());
    assertFalse(registered);
    assertEquals(0, counter.get());
  }

  @Test
  void registerSyncDefinitions_shouldRegisterAllDefinitions() {
    AtomicInteger registerCounter = new AtomicInteger();
    AtomicInteger callbackCounter = new AtomicInteger();
    boolean registered =
        ComponentRegistrationSupport.registerSyncDefinitions(
            List.of(new Def("a", "A"), new Def("b", "B")),
            def -> registerCounter.incrementAndGet(),
            def -> callbackCounter.incrementAndGet());
    assertTrue(registered);
    assertEquals(2, registerCounter.get());
    assertEquals(2, callbackCounter.get());
  }

  @Test
  void registerAsyncDefinitions_shouldRegisterAllDefinitions() {
    Logger logger = mock(Logger.class);
    AtomicInteger callbackCounter = new AtomicInteger();
    boolean registered =
        ComponentRegistrationSupport.registerAsyncDefinitions(
            List.of(new Def("a", "A"), new Def("b", "B")),
            def -> Mono.empty(),
            Def::name,
            "McpDef",
            logger,
            def -> callbackCounter.incrementAndGet());
    assertTrue(registered);
    assertEquals(2, callbackCounter.get());
  }

  @Test
  void registerAsyncDefinitions_shouldWrapFailure() {
    Logger logger = mock(Logger.class);
    McpServerComponentRegistrationException exception =
        assertThrows(
            McpServerComponentRegistrationException.class,
            () ->
                ComponentRegistrationSupport.registerAsyncDefinitions(
                    List.of(new Def("a", "A")),
                    def -> Mono.error(new RuntimeException("boom")),
                    Def::name,
                    "McpDef",
                    logger,
                    def -> {}));
    assertEquals("Failed to register async McpDef a", exception.getMessage());
  }

  private record Def(String name, String sourceMethod) {}
}
