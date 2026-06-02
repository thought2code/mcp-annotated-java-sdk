package com.github.thought2code.mcp.annotated.server.component;

import io.modelcontextprotocol.spec.McpSchema;

/**
 * Canonical error-message templates for duplicate MCP component registrations.
 *
 * <p>Used by the annotation processor at compile time and by runtime registration helpers so
 * duplicate tool, prompt, resource, and completion bindings produce consistent diagnostics.
 *
 * @author codeboyzhou
 */
public final class DuplicateComponentMessageHelper {

  private DuplicateComponentMessageHelper() {}

  /**
   * @param name duplicate MCP tool name
   * @param previousMethod first registering source method
   * @param currentMethod colliding source method
   * @return formatted duplicate-tool message
   */
  public static String duplicateToolName(String name, String previousMethod, String currentMethod) {
    return String.format(
        "Duplicate tool name '%s' found for methods %s and %s",
        name, previousMethod, currentMethod);
  }

  /**
   * @param name duplicate MCP prompt name
   * @param previousMethod first registering source method
   * @param currentMethod colliding source method
   * @return formatted duplicate-prompt message
   */
  public static String duplicatePromptName(
      String name, String previousMethod, String currentMethod) {
    return String.format(
        "Duplicate prompt name '%s' found for methods %s and %s",
        name, previousMethod, currentMethod);
  }

  /**
   * @param name duplicate MCP resource name
   * @param previousMethod first registering source method
   * @param currentMethod colliding source method
   * @return formatted duplicate-resource message
   */
  public static String duplicateResourceName(
      String name, String previousMethod, String currentMethod) {
    return String.format(
        "Duplicate resource name '%s' found for methods %s and %s",
        name, previousMethod, currentMethod);
  }

  /**
   * @param referenceDescription human-readable completion reference (see {@link
   *     #completionReferenceDescription})
   * @param previousMethod first registering source method
   * @param currentMethod colliding source method
   * @return formatted duplicate-completion message
   */
  public static String duplicateCompletionReference(
      String referenceDescription, String previousMethod, String currentMethod) {
    return String.format(
        "Duplicate completion reference %s found for methods %s and %s",
        referenceDescription, previousMethod, currentMethod);
  }

  /**
   * Describes a completion reference for error messages.
   *
   * @param reference MCP completion reference; may be {@code null}
   * @return description such as {@code prompt name 'foo'} or {@code resource uri 'bar'}
   */
  public static String completionReferenceDescription(McpSchema.CompleteReference reference) {
    if (reference instanceof McpSchema.ResourceReference resourceReference) {
      return completionResourceReferenceDescription(resourceReference.uri());
    }
    if (reference instanceof McpSchema.PromptReference promptReference) {
      return completionPromptReferenceDescription(promptReference.name());
    }
    return reference == null ? "'null'" : "'" + reference + "'";
  }

  /**
   * @param promptName prompt name targeted by completion
   * @return formatted prompt-reference description
   */
  public static String completionPromptReferenceDescription(String promptName) {
    return "prompt name '" + promptName + "'";
  }

  /**
   * @param resourceUri resource URI targeted by completion
   * @return formatted resource-reference description
   */
  public static String completionResourceReferenceDescription(String resourceUri) {
    return "resource uri '" + resourceUri + "'";
  }
}
