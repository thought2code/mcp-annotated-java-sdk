package com.github.thought2code.mcp.annotated.server.component.completion;

import java.util.Collection;
import java.util.List;

/**
 * Represents a completion result for MCP (Model Context Protocol) server operations.
 *
 * <p>This record encapsulates the response data for completion requests, including the list of
 * completion values, total count, and whether more results are available. The class is designed to
 * be immutable and provides defensive copying to protect internal state from external modification.
 *
 * @param values the list of completion values, may be null
 * @param total the total number of available completions, may be null
 * @param hasMore true if more completions are available, false otherwise
 * @author codeboyzhou
 */
public record CompletionResult(List<String> values, Integer total, boolean hasMore) {
  /**
   * Compact constructor that creates a defensive copy of the values list.
   *
   * <p>This constructor ensures that the internal list cannot be modified from outside the record
   * by creating an immutable copy using {@link List#copyOf(Collection)}. If the input list is null,
   * it remains null to preserve the original intent.
   *
   * @param values the input list of completion values to be defensively copied
   * @param total the total number of available completions, may be null
   * @param hasMore true if more completions are available, false otherwise
   */
  public CompletionResult {
    values = values == null ? null : List.copyOf(values);
  }

  /**
   * Creates a new {@link Builder} instance for constructing {@link CompletionResult} objects.
   *
   * <p>This factory method provides a convenient way to create a builder for step-by-step
   * construction of completion results using the builder pattern.
   *
   * @return a new {@link Builder} instance
   */
  public static CompletionResult.Builder builder() {
    return new CompletionResult.Builder();
  }

  /**
   * Creates an empty {@link CompletionResult} instance.
   *
   * <p>This factory method returns a completion result with no values, zero total count, and no
   * more results available. Useful as a default or placeholder completion.
   *
   * @return an empty {@link CompletionResult} instance
   */
  public static CompletionResult empty() {
    return builder().values(List.of()).total(0).hasMore(false).build();
  }

  /**
   * Builder class for constructing {@link CompletionResult} instances.
   *
   * <p>This builder provides a fluent API for creating completion results with defensive copying to
   * protect against external modification of mutable inputs. The builder maintains the same
   * immutability guarantees as the record itself.
   *
   * @author codeboyzhou
   */
  public static class Builder {
    /** The list of completion values. */
    private List<String> values;

    /** The total number of available completions. */
    private Integer total;

    /** Whether more completions are available. */
    private boolean hasMore;

    /**
     * Sets the completion values with defensive copying.
     *
     * <p>This method creates an immutable copy of the input list to prevent external modification
     * after the value has been set. If the input list is null, the internal values field is set to
     * null.
     *
     * @param values the list of completion values to set, may be null
     * @return this {@link Builder} instance for method chaining
     */
    public Builder values(List<String> values) {
      // Create defensive copy to prevent external modification after setting
      this.values = values == null ? null : List.copyOf(values);
      return this;
    }

    /**
     * Sets the total number of available completions.
     *
     * <p>This value represents the total count of completions that could be returned, which may be
     * greater than the number of values in the current completion result if pagination is
     * supported.
     *
     * @param total the total number of available completions, may be null
     * @return this {@link Builder} instance for method chaining
     */
    public Builder total(Integer total) {
      this.total = total;
      return this;
    }

    /**
     * Sets whether more completions are available.
     *
     * <p>This flag indicates whether additional completion results can be obtained through further
     * requests, typically used for pagination or incremental loading scenarios.
     *
     * @param hasMore true if more completions are available, false otherwise
     * @return this {@link Builder} instance for method chaining
     */
    public Builder hasMore(boolean hasMore) {
      this.hasMore = hasMore;
      return this;
    }

    /**
     * Builds a new {@link CompletionResult} instance with the configured values.
     *
     * <p>This method creates a new record instance using the current builder state. The record's
     * compact constructor will apply additional defensive copying to ensure the final object is
     * fully immutable.
     *
     * @return a new {@link CompletionResult} instance with the configured values
     */
    public CompletionResult build() {
      return new CompletionResult(values, total, hasMore);
    }
  }
}
