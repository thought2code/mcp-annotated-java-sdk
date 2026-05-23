package com.github.thought2code.mcp.annotated.server.component;

import com.github.thought2code.mcp.annotated.reflect.MethodInvoker;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Default component instance factory used by the SDK.
 *
 * <p>The factory creates component instances lazily through their no-argument constructor and
 * caches one instance per component class for the lifetime of the application context.
 *
 * @author codeboyzhou
 */
public final class DefaultMcpComponentInstanceFactory implements McpComponentInstanceFactory {
  /** Component instance cache. */
  private final ConcurrentMap<Class<?>, Object> instances = new ConcurrentHashMap<>();

  private DefaultMcpComponentInstanceFactory() {}

  /**
   * Creates an empty default instance factory.
   *
   * @return a default component instance factory
   */
  public static DefaultMcpComponentInstanceFactory create() {
    return new DefaultMcpComponentInstanceFactory();
  }

  @Override
  public Object getInstance(Class<?> componentClass) {
    Objects.requireNonNull(componentClass, "componentClass must not be null");
    return instances.computeIfAbsent(componentClass, MethodInvoker::createInstance);
  }
}
