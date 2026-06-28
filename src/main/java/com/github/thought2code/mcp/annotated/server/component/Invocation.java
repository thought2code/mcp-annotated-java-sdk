package com.github.thought2code.mcp.annotated.server.component;

import com.github.thought2code.mcp.annotated.util.JacksonHelper;
import org.jetbrains.annotations.NotNull;

/**
 * Outcome of invoking one generated MCP component method.
 *
 * <p>Generated invokers populate {@link #result} with the Java return value (or a stand-in message
 * for {@code void} or {@code null} returns) and set {@link #isError} when the invocation failed and
 * should be surfaced to the MCP client as an error response.
 *
 * @param result invocation return value or error text; never {@code null} at the record level
 * @param isError {@code true} when the invocation failed and MCP should treat the result as an
 *     error
 * @author codeboyzhou
 */
public record Invocation(@NotNull Object result, boolean isError) {
  /**
   * Returns a new instance of {@code Builder} for creating a new {@code Invocation}.
   *
   * @return a new instance of {@code Builder}
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Returns the invocation result as text.
   *
   * @return the string representation of the invocation result
   */
  public String asText() {
    return result.toString();
  }

  /**
   * Returns the invocation result serialized as JSON.
   *
   * @return the JSON representation of the invocation result
   */
  public String asJson() {
    return JacksonHelper.toJsonString(result);
  }

  /** Mutable builder for {@link Invocation} used by generated invokers. */
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
     * Builds a new instance of {@code Invocation} with the configured values.
     *
     * @return a new instance of {@code Invocation}
     */
    public Invocation build() {
      return new Invocation(result, isError);
    }
  }
}
