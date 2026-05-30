package com.github.thought2code.mcp.annotated;

import com.github.thought2code.mcp.annotated.reflect.ReflectionsProvider;
import com.github.thought2code.mcp.annotated.server.component.DefaultMcpComponentInstanceFactory;
import com.github.thought2code.mcp.annotated.server.component.McpComponentInstanceFactory;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Set;

/**
 * Runtime context for one annotated MCP application.
 *
 * <p>The context owns application-scoped services such as classpath scanning. This keeps
 * independent MCP applications from sharing global mutable state when they run in the same JVM.
 */
public final class McpApplicationContext {
  /** The reflections provider used to scan the application's classpath for annotated components. */
  private final ReflectionsProvider reflectionsProvider;

  /** The factory used to create or locate annotated component instances. */
  private final McpComponentInstanceFactory componentInstanceFactory;

  private McpApplicationContext(
      ReflectionsProvider reflectionsProvider,
      McpComponentInstanceFactory componentInstanceFactory) {
    this.reflectionsProvider =
        Objects.requireNonNull(reflectionsProvider, "reflectionsProvider must not be null");
    this.componentInstanceFactory =
        Objects.requireNonNull(
            componentInstanceFactory, "componentInstanceFactory must not be null");
  }

  /**
   * Creates a new context for the specified MCP application class.
   *
   * @param mainClass the application entry class used for package scanning
   * @return a new application-scoped context
   */
  public static McpApplicationContext from(Class<?> mainClass) {
    ReflectionsProvider reflectionsProvider =
        ReflectionsProvider.initializeReflectionsInstance(mainClass);
    return new McpApplicationContext(
        reflectionsProvider, DefaultMcpComponentInstanceFactory.create());
  }

  /**
   * Returns all methods annotated with the specified annotation within this context's scan scope.
   *
   * @param annotation the annotation class to search for
   * @return methods annotated with the given annotation
   */
  public Set<Method> getMethodsAnnotatedWith(Class<? extends Annotation> annotation) {
    return reflectionsProvider.getMethodsAnnotatedWith(annotation);
  }

  /**
   * Returns all fields annotated with the specified annotation within this context's scan scope.
   *
   * @param annotation the annotation class to search for
   * @return fields annotated with the given annotation
   */
  public Set<Field> getFieldsAnnotatedWith(Class<? extends Annotation> annotation) {
    return reflectionsProvider.getFieldsAnnotatedWith(annotation);
  }

  /**
   * Returns a component instance for the specified component class.
   *
   * @param componentClass the class declaring annotated MCP component methods
   * @return the component instance to invoke
   */
  public Object getComponentInstance(Class<?> componentClass) {
    return componentInstanceFactory.getInstance(componentClass);
  }
}
