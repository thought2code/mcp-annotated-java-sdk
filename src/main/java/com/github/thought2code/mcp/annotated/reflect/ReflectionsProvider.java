package com.github.thought2code.mcp.annotated.reflect;

import com.github.thought2code.mcp.annotated.annotation.McpServerApplication;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

import static org.reflections.scanners.Scanners.FieldsAnnotated;
import static org.reflections.scanners.Scanners.MethodsAnnotated;

/**
 * A provider class for reflection operations using the Reflections library.
 *
 * <p>This class provides static methods for initializing and accessing reflection capabilities to
 * scan for annotated methods and fields in a specified package. It uses the Reflections library to
 * perform runtime scanning of classpath components.
 *
 * <p>Each provider instance owns one {@link Reflections} instance initialized with a base package
 * derived from the main application class or the {@link McpServerApplication} annotation. The
 * scanning is configured to look for annotated methods and fields.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Initializes reflection scanning for a specified base package
 *   <li>Supports package configuration via {@link McpServerApplication} annotation
 *   <li>Retrieves methods annotated with specific annotations
 *   <li>Retrieves fields annotated with specific annotations
 * </ul>
 *
 * @author codeboyzhou
 * @see Reflections
 * @see McpServerApplication
 */
public final class ReflectionsProvider {

  private static final Logger log = LoggerFactory.getLogger(ReflectionsProvider.class);

  /** The {@link Reflections} instance used for scanning and reflection operations. */
  private final Reflections reflections;

  /**
   * Initializes the {@link Reflections} instance with the specified main class.
   *
   * <p>This method determines the base package for reflection scanning by examining the provided
   * main class. The base package can be configured in three ways:
   *
   * <ol>
   *   <li>Default: Uses the package name of the main class
   *   <li>Annotation string: Uses the {@code basePackage} attribute from {@link
   *       McpServerApplication} if specified and not blank
   *   <li>Annotation class: Uses the package name of the {@code basePackageClass} attribute from
   *       {@link McpServerApplication} if specified and not {@code Object.class}
   * </ol>
   *
   * <p>The Reflections instance is configured to scan for annotated methods and fields within the
   * determined base package.
   *
   * @param mainClass the main application class used to determine the base package
   * @see McpServerApplication
   * @see Reflections
   */
  private ReflectionsProvider(Class<?> mainClass) {
    log.info("Initializing Reflections instance");
    String basePackage = mainClass.getPackageName();
    McpServerApplication application = mainClass.getAnnotation(McpServerApplication.class);
    if (application != null) {
      if (!application.basePackage().trim().isBlank()) {
        basePackage = application.basePackage();
      }
      if (application.basePackageClass() != Object.class) {
        basePackage = application.basePackageClass().getPackageName();
      }
    }
    reflections = new Reflections(basePackage, MethodsAnnotated, FieldsAnnotated);
    log.info("Reflections instance initialized successfully");
  }

  /**
   * Creates a new provider initialized for the specified main class.
   *
   * @param mainClass the main application class used to determine the base package
   * @return a new reflection provider
   */
  public static ReflectionsProvider initializeReflectionsInstance(Class<?> mainClass) {
    return new ReflectionsProvider(mainClass);
  }

  /**
   * Retrieves all methods annotated with the specified annotation.
   *
   * <p>This method uses the initialized Reflections instance to scan the configured base package
   * and return a set of all methods that are annotated with the given annotation type.
   *
   * <p>The method requires that {@link #initializeReflectionsInstance(Class)} has been called
   * before invoking this method.
   *
   * @param annotation the annotation class to search for
   * @return a set of methods annotated with the specified annotation
   * @see Reflections#getMethodsAnnotatedWith(Class)
   * @see Method
   */
  public Set<Method> getMethodsAnnotatedWith(Class<? extends Annotation> annotation) {
    return reflections.getMethodsAnnotatedWith(annotation);
  }

  /**
   * Retrieves all fields annotated with the specified annotation.
   *
   * <p>This method uses the initialized Reflections instance to scan the configured base package
   * and return a set of all fields that are annotated with the given annotation type.
   *
   * <p>The method requires that {@link #initializeReflectionsInstance(Class)} has been called
   * before invoking this method.
   *
   * @param annotation the annotation class to search for
   * @return a set of fields annotated with the specified annotation
   * @see Reflections#getFieldsAnnotatedWith(Class)
   * @see Field
   */
  public Set<Field> getFieldsAnnotatedWith(Class<? extends Annotation> annotation) {
    return reflections.getFieldsAnnotatedWith(annotation);
  }
}
