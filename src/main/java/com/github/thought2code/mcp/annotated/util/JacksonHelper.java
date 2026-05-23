package com.github.thought2code.mcp.annotated.util;

import com.github.thought2code.mcp.annotated.enums.McpServerError;
import com.github.thought2code.mcp.annotated.exception.McpServerConfigurationException;
import com.github.thought2code.mcp.annotated.exception.McpServerException;
import java.io.File;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;

/**
 * Helper class for Jackson JSON and YAML serialization and deserialization.
 *
 * <p>This class provides static methods for serializing and deserializing objects to and from JSON
 * and YAML formats using Jackson.
 *
 * @author codeboyzhou
 */
public final class JacksonHelper {

  private static final Logger log = LoggerFactory.getLogger(JacksonHelper.class);

  /** JSON ObjectMapper instance. */
  private static final ObjectMapper JSON = new ObjectMapper(new JsonFactory());

  /** YAML ObjectMapper instance. */
  private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

  /**
   * Private constructor to prevent instantiation of the utility class.
   *
   * @throws UnsupportedOperationException if instantiation is attempted
   */
  @VisibleForTesting
  JacksonHelper() {
    throw new UnsupportedOperationException("Utility class should not be instantiated");
  }

  /**
   * Serialize an object to a JSON string.
   *
   * @param object the object to serialize
   * @return the JSON string representation of the object, or {@code null} when {@code object} is
   *     {@code null}
   * @throws McpServerException if serialization fails
   */
  public static String toJsonString(Object object) {
    if (object == null) {
      return null;
    }

    try {
      return JSON.writeValueAsString(object);
    } catch (JacksonException e) {
      log.error("Error serializing object to JSON", e);
      throw new McpServerException(McpServerError.JSON_SERIALIZE_ERROR.toString(), e);
    }
  }

  /**
   * Deserialize a JSON string to an object of the specified type.
   *
   * @param json the JSON string to deserialize
   * @param valueType the class of the object to deserialize to
   * @param <T> the type of the object to deserialize to
   * @return the deserialized object, or {@code null} when {@code json} is {@code null}
   * @throws McpServerException if deserialization fails
   */
  public static <T> T fromJson(String json, Class<T> valueType) {
    if (json == null) {
      return null;
    }

    try {
      return JSON.readValue(json, valueType);
    } catch (JacksonException e) {
      log.error("Error deserializing JSON to object", e);
      throw new McpServerException(McpServerError.JSON_DESERIALIZE_ERROR.toString(), e);
    }
  }

  /**
   * Deserialize a YAML file to an object of the specified type.
   *
   * @param yamlFile the YAML file to deserialize
   * @param valueType the class of the object to deserialize to
   * @param <T> the type of the object to deserialize to
   * @return the deserialized object
   */
  public static <T> T fromYaml(File yamlFile, Class<T> valueType) {
    try {
      return YAML.readValue(yamlFile, valueType);
    } catch (JacksonException e) {
      final String path = yamlFile.getAbsolutePath();
      throw new McpServerConfigurationException(McpServerError.YAML_READ_ERROR.withDetail(path), e);
    }
  }
}
