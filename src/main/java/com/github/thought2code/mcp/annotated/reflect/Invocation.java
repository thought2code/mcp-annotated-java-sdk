package com.github.thought2code.mcp.annotated.reflect;

import org.jetbrains.annotations.NotNull;

/**
 * This record represents the result of reflection invocation of Java method.
 *
 * @author codeboyzhou
 */
public record Invocation(@NotNull Object result, boolean isError) {
  /**
   * Returns a new instance of {@code Builder} for creating a new {@code InvocationResult}.
   *
   * @return a new instance of {@code Builder}
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Returns the invocation result as text. */
  public String asText() {
    return result.toString();
  }

  /**
   * This class implements the builder pattern for creating a new instance of {@code
   * InvocationResult}.
   *
   * @author codeboyzhou
   */
  public static final class Builder {

    /** The result of the invocation. */
    private Object result;

    /** Indicates whether an exception occurred during the invocation. */
    private boolean isError;

    /**
     * Sets the result of the invocation.
     *
     * @param result the result of the invocation
     * @return the builder instance
     */
    public Builder result(Object result) {
      this.result = result;
      return this;
    }

    /**
     * Sets whether an exception occurred during the invocation.
     *
     * @param isError {@code true} if an exception occurred, {@code false} otherwise
     * @return the builder instance
     */
    public Builder isError(boolean isError) {
      this.isError = isError;
      return this;
    }

    /**
     * Builds a new instance of {@code InvocationResult} with the configured values.
     *
     * @return a new instance of {@code InvocationResult}
     */
    public Invocation build() {
      return new Invocation(result, isError);
    }
  }
}
