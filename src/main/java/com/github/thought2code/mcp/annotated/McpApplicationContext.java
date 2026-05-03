package com.github.thought2code.mcp.annotated;

import com.github.thought2code.mcp.annotated.reflect.ReflectionsProvider;
import com.github.thought2code.mcp.annotated.server.component.DefaultMcpComponentInstanceFactory;
import com.github.thought2code.mcp.annotated.server.component.McpComponentInstanceFactory;
import com.github.thought2code.mcp.annotated.server.component.ResourceBundleProvider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Set;

/**
 * Runtime context for one annotated MCP application.
 *
 * <p>The context owns application-scoped services such as classpath scanning and i18n lookup. This
 * keeps independent MCP applications from sharing global mutable state when they run in the same
 * JVM.
 */
public final class McpApplicationContext {
  /** The reflections provider used to scan the application's classpath for annotated components. */
  private final ReflectionsProvider reflectionsProvider;

  /** The resource bundle provider used to load i18n messages. */
  private final ResourceBundleProvider resourceBundleProvider;

  /** The factory used to create or locate annotated component instances. */
  private final McpComponentInstanceFactory componentInstanceFactory;

  private McpApplicationContext(
      ReflectionsProvider reflectionsProvider,
      ResourceBundleProvider resourceBundleProvider,
      McpComponentInstanceFactory componentInstanceFactory) {
    this.reflectionsProvider =
        Objects.requireNonNull(reflectionsProvider, "reflectionsProvider must not be null");
    this.resourceBundleProvider =
        Objects.requireNonNull(resourceBundleProvider, "resourceBundleProvider must not be null");
    this.componentInstanceFactory =
        Objects.requireNonNull(
            componentInstanceFactory, "componentInstanceFactory must not be null");
  }

  /**
   * Creates a new context for the specified MCP application class.
   *
   * @param mainClass the application entry class used for package scanning and i18n configuration
   * @return a new application-scoped context
   */
  public static McpApplicationContext from(Class<?> mainClass) {
    ReflectionsProvider reflectionsProvider =
        ReflectionsProvider.initializeReflectionsInstance(mainClass);
    ResourceBundleProvider resourceBundleProvider =
        ResourceBundleProvider.loadResourceBundle(mainClass);
    return new McpApplicationContext(
        reflectionsProvider, resourceBundleProvider, DefaultMcpComponentInstanceFactory.create());
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
   * Resolves a localized string through this context's resource bundle.
   *
   * @param i18nKey the i18n key or literal value
   * @param defaultValue the fallback value
   * @return the localized or fallback value
   */
  public String getLocalizedString(String i18nKey, String defaultValue) {
    return resourceBundleProvider.getString(i18nKey, defaultValue);
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
