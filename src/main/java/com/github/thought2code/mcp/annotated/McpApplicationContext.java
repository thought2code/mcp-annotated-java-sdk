package com.github.thought2code.mcp.annotated;

import com.github.thought2code.mcp.annotated.annotation.McpServerApplication;
import com.github.thought2code.mcp.annotated.enums.McpServerError;
import com.github.thought2code.mcp.annotated.exception.McpServerException;
import com.github.thought2code.mcp.annotated.util.StringHelper;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Runtime context for one annotated MCP application.
 *
 * <p>In the build-time component architecture, this context no longer performs runtime classpath
 * discovery. Instead, it centralizes runtime concerns that component invokers still need:
 *
 * <ul>
 *   <li>Component instance lifecycle (lazy creation with one cached instance per component class)
 *   <li>Scope filtering for component model definitions based on the resolved application base
 *       package
 * </ul>
 *
 * <p>Each application gets its own context instance so multiple MCP applications can run in the
 * same JVM without sharing component instances or scope rules.
 */
public final class McpApplicationContext {
  /** Base package resolved from {@link McpServerApplication}. */
  private final String basePackage;

  /** Component instance cache; one instance per component class. */
  private final ConcurrentMap<Class<?>, Object> componentInstances = new ConcurrentHashMap<>();

  private McpApplicationContext(String basePackage) {
    this.basePackage = Objects.requireNonNull(basePackage, "basePackage must not be null");
  }

  /**
   * Creates a new context for the specified MCP application class.
   *
   * <p>The resolved base package follows this priority:
   *
   * <ol>
   *   <li>{@link McpServerApplication#basePackageClass()} when explicitly set
   *   <li>{@link McpServerApplication#basePackage()} when non-blank
   *   <li>the package of {@code mainClass} as fallback
   * </ol>
   *
   * @param mainClass the application entry class used to resolve base-package scope
   * @return a new application-scoped context
   */
  public static McpApplicationContext from(Class<?> mainClass) {
    Objects.requireNonNull(mainClass, "mainClass must not be null");
    return new McpApplicationContext(resolveBasePackage(mainClass));
  }

  /**
   * Returns a component instance for the specified component class.
   *
   * <p>Instances are created lazily via the no-argument constructor and cached per declaring class.
   * Repeated calls for the same class return the same instance.
   *
   * @param componentClass the class declaring annotated MCP component methods
   * @return the component instance to invoke
   */
  public Object getComponentInstance(Class<?> componentClass) {
    Objects.requireNonNull(componentClass, "componentClass must not be null");
    return componentInstances.computeIfAbsent(
        componentClass, McpApplicationContext::createInstance);
  }

  /**
   * Returns whether a component source method belongs to this application's base package scope.
   *
   * <p>This method is used while loading component definitions to prevent out-of-scope components
   * from being registered (for example, fixtures from other test packages or neighboring modules).
   *
   * @param sourceMethod component source method descriptor in the form {@code fqcn#method(...)}
   * @return {@code true} when the source method declaring class is within the configured base
   *     package
   */
  public boolean isInScope(String sourceMethod) {
    if (StringHelper.isBlank(sourceMethod)) {
      return false;
    }

    final int hashIndex = sourceMethod.indexOf(StringHelper.HASH);
    final String declaringClassName =
        (hashIndex >= 0 ? sourceMethod.substring(0, hashIndex) : sourceMethod).trim();
    if (declaringClassName.isEmpty()) {
      return false;
    }

    if (basePackage.isEmpty()) {
      return !declaringClassName.contains(StringHelper.DOT);
    }
    return declaringClassName.startsWith(basePackage + StringHelper.DOT);
  }

  /**
   * Resolves the base package used for component-definition scope filtering.
   *
   * <p>Resolution priority:
   *
   * <ol>
   *   <li>{@link McpServerApplication#basePackageClass()} when explicitly configured
   *   <li>{@link McpServerApplication#basePackage()} when non-blank
   *   <li>{@code mainClass.getPackageName()} as fallback
   * </ol>
   *
   * @param mainClass application entry class
   * @return resolved base package (never {@code null})
   */
  private static String resolveBasePackage(Class<?> mainClass) {
    McpServerApplication application = mainClass.getAnnotation(McpServerApplication.class);
    if (application == null) {
      return mainClass.getPackageName();
    }

    if (application.basePackageClass() != Object.class) {
      return application.basePackageClass().getPackageName();
    }

    final String configuredBasePackage = application.basePackage().trim();
    return configuredBasePackage.isBlank() ? mainClass.getPackageName() : configuredBasePackage;
  }

  /**
   * Creates a component instance using the class no-argument constructor.
   *
   * <p>This method is used by the context cache on first access for each component class.
   *
   * @param clazz component class declaring MCP annotated methods
   * @return created component instance
   * @throws McpServerException when instance creation fails
   */
  private static Object createInstance(Class<?> clazz) {
    try {
      return clazz.getDeclaredConstructor().newInstance();
    } catch (Exception e) {
      throw new McpServerException(
          McpServerError.COMPONENT_INSTANCE_CREATE_ERROR.withDetail(clazz.getName()), e);
    }
  }
}
