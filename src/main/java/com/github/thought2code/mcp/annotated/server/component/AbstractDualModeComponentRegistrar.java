package com.github.thought2code.mcp.annotated.server.component;

import com.github.thought2code.mcp.annotated.McpApplicationContext;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpSyncServer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
   */
  protected abstract void addAsyncSpecification(McpAsyncServer server, A specification);

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
      addAsyncSpecification(server, specification);
      final String specificationName = asyncSpecificationName(specification);
      log.debug("Async {} {} registered successfully", annotationTypeName, specificationName);
    }
  }
}
