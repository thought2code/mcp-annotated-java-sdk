package com.github.thought2code.mcp.annotated.server.component;

/**
 * Strategy interface for creating or locating annotated MCP component instances.
 *
 * <p>Implementations can integrate with dependency injection containers, return prebuilt objects,
 * or apply custom lifecycle rules. The default SDK implementation creates one no-argument instance
 * per component class and reuses it for all annotated methods in the same application context.
 *
 * @author codeboyzhou
 */
@FunctionalInterface
public interface McpComponentInstanceFactory {
  /**
   * Returns an instance for the specified component class.
   *
   * @param componentClass the class declaring an annotated MCP component method
   * @return the component instance to invoke
   */
  Object getInstance(Class<?> componentClass);
}
