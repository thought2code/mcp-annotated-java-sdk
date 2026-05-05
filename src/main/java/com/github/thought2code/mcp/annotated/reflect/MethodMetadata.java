package com.github.thought2code.mcp.annotated.reflect;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class caches method parameters and return type by method signature to avoid repeated
 * reflection lookups on hot invocation paths.
 *
 * @author codeboyzhou
 */
public final class MethodMetadata {

  private static final Logger log = LoggerFactory.getLogger(MethodMetadata.class);

  /** Cache of method metadata by method signature. */
  private static final Map<String, MethodMetadata> CACHE = new ConcurrentHashMap<>();

  /** Cached method parameters. */
  private final Parameter[] parameters;

  /** Cached method return type. */
  private final Class<?> returnType;

  /** Captures parameters and return type from the given method. */
  private MethodMetadata(Method method) {
    this.parameters = method.getParameters();
    this.returnType = method.getReturnType();
  }

  /**
   * Returns cached metadata for the given method, creating it on first access.
   *
   * @param method method to resolve metadata for
   * @return cached metadata for the method
   */
  public static MethodMetadata of(Method method) {
    final String methodGenericString = method.toGenericString();
    log.debug("Caching metadata of method: {}", methodGenericString);
    return CACHE.computeIfAbsent(methodGenericString, key -> new MethodMetadata(method));
  }

  /**
   * Returns a defensive copy of cached parameters.
   *
   * @return cloned parameter array
   */
  public Parameter[] getParameters() {
    // defensive copy to avoid SpotBugs warning about mutable array
    return parameters == null ? new Parameter[0] : parameters.clone();
  }

  /**
   * Returns the cached method return type.
   *
   * @return return type of the method
   */
  public Class<?> getReturnType() {
    return returnType;
  }
}
