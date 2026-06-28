package com.github.thought2code.mcp.annotated.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.thought2code.mcp.annotated.configuration.ServerConfiguration;
import com.github.thought2code.mcp.annotated.enums.McpServerError;
import com.github.thought2code.mcp.annotated.exception.McpServerException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
  void fromYaml_shouldDeserializeValidStream() {
    InputStream yaml =
        new ByteArrayInputStream("name: test\nage: 25".getBytes(StandardCharsets.UTF_8));

    Person person = JacksonHelper.fromYaml(yaml, "test-stream.yaml", Person.class);

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
  void fromYaml_shouldThrowConfigurationExceptionWhenStreamIsInvalid() {
    InputStream yaml = new ByteArrayInputStream("name: [".getBytes(StandardCharsets.UTF_8));

    McpServerException exception =
        assertThrows(
            McpServerException.class,
            () -> JacksonHelper.fromYaml(yaml, "invalid-stream.yaml", Map.class));

    assertTrue(exception.getMessage().contains(McpServerError.YAML_READ_ERROR.getCode()));
    assertTrue(exception.getMessage().contains("invalid-stream.yaml"));
  }

  @Test
  void mergeYaml_shouldMergeNestedProfileOverridesIntoBaseConfiguration() throws Exception {
    InputStream baseStream = classpathResourceStream("test-mcp-server-with-profile.yml");
    InputStream profileStream = classpathResourceStream("test-mcp-server-with-profile-dev.yml");

    ServerConfiguration configuration =
        JacksonHelper.fromYaml(
            baseStream, "test-mcp-server-with-profile.yml", ServerConfiguration.class);
    configuration =
        JacksonHelper.mergeYaml(
            configuration,
            profileStream,
            "test-mcp-server-with-profile-dev.yml",
            ServerConfiguration.class);

    assertEquals("mcp-server-dev", configuration.name());
    assertFalse(configuration.capabilities().resource());
    assertTrue(configuration.capabilities().prompt());
    assertEquals("/mcp/message/dev", configuration.streamable().mcpEndpoint());
    assertEquals(9004, configuration.streamable().port());
  }

  private static InputStream classpathResourceStream(String fileName) throws IOException {
    InputStream resource = JacksonHelperTest.class.getClassLoader().getResourceAsStream(fileName);
    if (resource == null) {
      throw new IllegalArgumentException("Missing classpath resource: " + fileName);
    }
    return resource;
  }
}
