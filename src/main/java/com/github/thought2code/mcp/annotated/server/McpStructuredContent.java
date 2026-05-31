package com.github.thought2code.mcp.annotated.server;

import io.modelcontextprotocol.spec.McpSchema;

/**
 * Marker interface for MCP tool method results that return structured content.
 *
 * <p>This interface is used to distinguish between simple text results and complex results
 * containing structured data. When an MCP tool method returns an object implementing this
 * interface, the MCP server will provide both text representation and structured data
 * representation.
 *
 * <h2>Usage Scenarios</h2>
 *
 * <ul>
 *   <li><strong>Simple Text Results</strong>: When tool methods return String or other primitive
 *       types, only text content is provided
 *   <li><strong>Structured Results</strong>: When tool methods return objects implementing this
 *       interface, both text and structured data are provided
 * </ul>
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * public class WeatherTool {
 *
 *   @McpTool(description = "Get weather information")
 *   public WeatherData getWeather(@McpToolParam(name = "city") String city) {
 *     final int temperature = 25;
 *     final String condition = "Sunny";
 *     return new WeatherData(city, temperature, condition);
 *   }
 *
 *   // Structured result class implementing McpStructuredContent
 *   public record WeatherData(
 *       @McpJsonSchemaProperty(description = "City name") String city,
 *       @McpJsonSchemaProperty(description = "Temperature") int temperature,
 *       @McpJsonSchemaProperty(description = "Condition") String condition)
 *       implements McpStructuredContent {
 *
 *     @Override
 *     public String asTextContent() {
 *       return String.format("City: %s, Temperature: %d°C, Condition: %s",
 *           city, temperature, condition);
 *     }
 *   }
 * }
 * }</pre>
 *
 * <h2>MCP Response Format</h2>
 *
 * When a tool returns an object implementing this interface, the MCP response will contain:
 *
 * <pre>{@code
 * {
 *   "content": [
 *     {
 *       "type": "text",
 *       "text": "City: New York, Temperature: 25°C, Condition: Sunny"
 *     }
 *   ],
 *   "structuredContent": {
 *     "city": "New York",
 *     "temperature": 25,
 *     "condition": "Sunny"
 *   }
 * }
 * }</pre>
 *
 * <h2>Implementation Requirements</h2>
 *
 * <ul>
 *   <li>Implementing classes must provide meaningful text representation for direct AI model
 *       consumption
 *   <li>Fields of implementing classes will be automatically serialized as structured content
 *   <li>Text content should be concise and clear for AI understanding
 *   <li>Structured content should contain complete detailed information
 * </ul>
 *
 * <h2>Default Implementation</h2>
 *
 * The interface provides a default implementation of {@link #asTextContent()} that returns the
 * result of {@link Object#toString()}. If the default implementation doesn't meet requirements,
 * implementing classes should override this method.
 *
 * @see McpSchema.CallToolResult MCP tool call result specification
 * @author codeboyzhou
 */
public interface McpStructuredContent {
  /**
   * Returns the text representation of this structured content.
   *
   * <p>The text representation should provide a concise summary of the content, suitable for direct
   * consumption by AI models. The result of this method will be used as the text content in the MCP
   * response's {@code content} field.
   *
   * <p>Implementation Guidelines
   *
   * <ul>
   *   <li><strong>Conciseness</strong>: Text should be brief, avoiding excessive details
   *   <li><strong>Readability</strong>: Use natural language that's easy for AI to understand
   *   <li><strong>Completeness</strong>: Include key information, but not necessarily all details
   *   <li><strong>Consistency</strong>: Text content should be consistent with structured data
   * </ul>
   *
   * <p>Examples For weather query results:
   *
   * <ul>
   *   <li><strong>Good implementation</strong>: "City: New York, Temperature: 25°C, Condition:
   *       Sunny"
   *   <li><strong>Poor implementation</strong>: Returning full JSON strings or overly simplified
   *       information
   * </ul>
   *
   * @return the text representation of the structured content, should not return {@code null}
   * @see Object#toString() default implementation uses this method
   */
  default String asTextContent() {
    return toString();
  }
}
