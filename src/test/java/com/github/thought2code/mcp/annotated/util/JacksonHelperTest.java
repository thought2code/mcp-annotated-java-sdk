package com.github.thought2code.mcp.annotated.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.thought2code.mcp.annotated.enums.McpServerError;
import com.github.thought2code.mcp.annotated.exception.McpServerConfigurationException;
import com.github.thought2code.mcp.annotated.exception.McpServerException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JacksonHelperTest {

  record Person(String name, int age) {}

  static class BrokenSerialization {
    public String getValue() {
      throw new IllegalStateException("serialization failed");
    }
  }

  @Test
  void constructor_shouldNotBeInstantiable() {
    assertThrows(UnsupportedOperationException.class, JacksonHelper::new);
  }

  @Test
  void toJsonString_shouldSerializeAndReturnNullForNullInput() {
    assertNull(JacksonHelper.toJsonString(null));
    String json = JacksonHelper.toJsonString(new Person("test", 25));
    assertTrue(json.contains("\"name\":\"test\""));
    assertTrue(json.contains("\"age\":25"));
  }

  @Test
  void fromJson_shouldDeserializeAndReturnNullForNullInput() {
    assertNull(JacksonHelper.fromJson(null, Person.class));
    Person person = JacksonHelper.fromJson("{\"name\":\"test\",\"age\":25}", Person.class);
    assertEquals("test", person.name());
    assertEquals(25, person.age);
  }

  @Test
  void fromYaml_shouldDeserializeValidFile() throws IOException {
    File tempYaml = File.createTempFile("jackson-helper", ".yaml");
    try (FileWriter writer = new FileWriter(tempYaml)) {
      writer.write("name: test\nage: 25");
    }
    Person person = JacksonHelper.fromYaml(tempYaml, Person.class);
    assertEquals("test", person.name());
    assertEquals(25, person.age);
  }

  @Test
  void fromJson_shouldThrowMcpServerExceptionOnInvalidJson() {
    McpServerException exception =
        assertThrows(
            McpServerException.class, () -> JacksonHelper.fromJson("{invalid", Person.class));
    assertTrue(exception.getMessage().contains(McpServerError.JSON_DESERIALIZE_ERROR.getCode()));
  }

  @Test
  void toJsonString_shouldThrowMcpServerExceptionOnSerializationFailure() {
    McpServerException exception =
        assertThrows(
            McpServerException.class, () -> JacksonHelper.toJsonString(new BrokenSerialization()));
    assertTrue(exception.getMessage().contains(McpServerError.JSON_SERIALIZE_ERROR.getCode()));
  }

  @Test
  void fromYaml_shouldThrowConfigurationExceptionWhenFileMissing() {
    File missing = new File("non-existent-jackson-helper.yaml");
    McpServerConfigurationException exception =
        assertThrows(
            McpServerConfigurationException.class,
            () -> JacksonHelper.fromYaml(missing, Map.class));
    assertTrue(exception.getMessage().contains(McpServerError.YAML_READ_ERROR.getCode()));
  }
}
