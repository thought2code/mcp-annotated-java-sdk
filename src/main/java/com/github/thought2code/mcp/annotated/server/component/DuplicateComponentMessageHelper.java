package com.github.thought2code.mcp.annotated.server.component;

import io.modelcontextprotocol.spec.McpSchema;

/** Shared duplicate-component error messages for compile-time and runtime checks. */
public final class DuplicateComponentMessageHelper {

  private DuplicateComponentMessageHelper() {}

  public static String duplicateToolName(String name, String previousMethod, String currentMethod) {
    return String.format(
        "Duplicate tool name '%s' found for methods %s and %s",
        name, previousMethod, currentMethod);
  }

  public static String duplicatePromptName(
      String name, String previousMethod, String currentMethod) {
    return String.format(
        "Duplicate prompt name '%s' found for methods %s and %s",
        name, previousMethod, currentMethod);
  }

  public static String duplicateResourceName(
      String name, String previousMethod, String currentMethod) {
    return String.format(
        "Duplicate resource name '%s' found for methods %s and %s",
        name, previousMethod, currentMethod);
  }

  public static String duplicateCompletionReference(
      String referenceDescription, String previousMethod, String currentMethod) {
    return String.format(
        "Duplicate completion reference %s found for methods %s and %s",
        referenceDescription, previousMethod, currentMethod);
  }

  public static String completionReferenceDescription(McpSchema.CompleteReference reference) {
    if (reference instanceof McpSchema.ResourceReference resourceReference) {
      return completionResourceReferenceDescription(resourceReference.uri());
    }
    if (reference instanceof McpSchema.PromptReference promptReference) {
      return completionPromptReferenceDescription(promptReference.name());
    }
    return reference == null ? "'null'" : "'" + reference + "'";
  }

  public static String completionPromptReferenceDescription(String promptName) {
    return "prompt name '" + promptName + "'";
  }

  public static String completionResourceReferenceDescription(String resourceUri) {
    return "resource uri '" + resourceUri + "'";
  }
}
