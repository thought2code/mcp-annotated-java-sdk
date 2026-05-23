package com.github.thought2code.mcp.annotated.server.component;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import com.github.thought2code.mcp.annotated.exception.McpServerComponentRegistrationException;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpSyncServer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/**
 * Template base class for component registrars that support both sync and async server modes.
 *
 * <p>This class centralizes the shared registration loop (annotation-based method discovery,
 * iteration, and registration logging) so concrete component registrars only provide
 * component-specific specification creation and server registration behavior.
 *
 * @param <S> synchronous specification type
 * @param <A> asynchronous specification type
 * @author codeboyzhou
 */
public abstract class AbstractDualModeComponentRegistrar<S, A> implements McpComponentRegistrar {

  private final Logger log = LoggerFactory.getLogger(getClass());

  /**
   * @return annotation type used to discover component methods
   */
  protected abstract Class<? extends Annotation> annotationType();

  /**
   * Creates a synchronous specification from an annotated method.
   *
   * @param method discovered component method
   * @param context application context
   * @return sync specification
   */
  protected abstract S createSyncSpecification(Method method, McpApplicationContext context);

  /**
   * Creates an asynchronous specification from an annotated method.
   *
   * @param method discovered component method
   * @param context application context
   * @return async specification
   */
  protected abstract A createAsyncSpecification(Method method, McpApplicationContext context);

  /**
   * Adds a synchronous specification to the server.
   *
   * @param server target sync server
   * @param specification specification to register
   */
  protected abstract void addSyncSpecification(McpSyncServer server, S specification);

  /**
   * Adds an asynchronous specification to the server.
   *
   * @param server target async server
   * @param specification specification to register
   * @return registration completion signal
   */
  protected abstract Mono<Void> addAsyncSpecification(McpAsyncServer server, A specification);

  /**
   * @param specification registered sync specification
   * @return display name used in success logs
   */
  protected abstract String syncSpecificationName(S specification);

  /**
   * @param specification registered async specification
   * @return display name used in success logs
   */
  protected abstract String asyncSpecificationName(A specification);

  @Override
  public final void register(McpSyncServer server, McpApplicationContext context) {
    Set<Method> methods = context.getMethodsAnnotatedWith(annotationType());
    for (Method method : methods) {
      final String annotationTypeName = annotationType().getSimpleName();
      log.debug("Registering {} method: {}", annotationTypeName, method.toGenericString());
      S specification = createSyncSpecification(method, context);
      addSyncSpecification(server, specification);
      final String specificationName = syncSpecificationName(specification);
      log.debug("Sync {} {} registered successfully", annotationTypeName, specificationName);
    }
  }

  @Override
  public final void register(McpAsyncServer server, McpApplicationContext context) {
    Set<Method> methods = context.getMethodsAnnotatedWith(annotationType());
    for (Method method : methods) {
      final String annotationTypeName = annotationType().getSimpleName();
      log.debug("Registering async {} method: {}", annotationTypeName, method.toGenericString());
      A specification = createAsyncSpecification(method, context);
      final String specificationName = asyncSpecificationName(specification);
      Mono<Void> registration = addAsyncSpecification(server, specification);
      awaitAsyncRegistration(registration, annotationTypeName, specificationName);
      log.debug("Async {} {} registered successfully", annotationTypeName, specificationName);
    }
  }

  /**
   * Waits for an asynchronous component registration to complete during server startup.
   *
   * <p>Async MCP server APIs return a {@link Mono} that completes when registration finishes. This
   * method blocks on that signal so registration behaves like the synchronous path: each component
   * is fully registered before the next one is processed and before the server starts handling
   * requests.
   *
   * <p>If registration fails, the error is logged and rethrown as a {@link
   * McpServerComponentRegistrationException} with component context.
   *
   * @param registration registration completion signal from {@link #addAsyncSpecification}
   * @param componentType annotation type name used in error messages (for example, {@code McpTool})
   * @param specificationName registered component name used in error messages
   * @throws McpServerComponentRegistrationException if registration fails
   */
  private void awaitAsyncRegistration(
      Mono<Void> registration, String componentType, String specificationName) {
    try {
      registration.block();
    } catch (RuntimeException e) {
      final String message =
          String.format("Failed to register async %s %s", componentType, specificationName);
      log.error(message, e);
      throw new McpServerComponentRegistrationException(message, e);
    }
  }
}
