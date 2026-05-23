package com.github.thought2code.mcp.annotated.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class JavaTypeToJsonSchemaMapperTest {

  @Test
  void getJsonSchemaType_shouldMapKnownJavaTypes() {
    assertEquals("string", JavaTypeToJsonSchemaMapper.getJsonSchemaType(String.class));
    assertEquals("integer", JavaTypeToJsonSchemaMapper.getJsonSchemaType(int.class));
    assertEquals("integer", JavaTypeToJsonSchemaMapper.getJsonSchemaType(Integer.class));
    assertEquals("number", JavaTypeToJsonSchemaMapper.getJsonSchemaType(long.class));
    assertEquals("number", JavaTypeToJsonSchemaMapper.getJsonSchemaType(Long.class));
    assertEquals("number", JavaTypeToJsonSchemaMapper.getJsonSchemaType(float.class));
    assertEquals("number", JavaTypeToJsonSchemaMapper.getJsonSchemaType(Float.class));
    assertEquals("number", JavaTypeToJsonSchemaMapper.getJsonSchemaType(double.class));
    assertEquals("number", JavaTypeToJsonSchemaMapper.getJsonSchemaType(Double.class));
    assertEquals("number", JavaTypeToJsonSchemaMapper.getJsonSchemaType(Number.class));
    assertEquals("boolean", JavaTypeToJsonSchemaMapper.getJsonSchemaType(boolean.class));
    assertEquals("boolean", JavaTypeToJsonSchemaMapper.getJsonSchemaType(Boolean.class));
    assertEquals("object", JavaTypeToJsonSchemaMapper.getJsonSchemaType(Object.class));
  }

  @Test
  void getJsonSchemaType_shouldDefaultToStringForUnknownTypes() {
    assertEquals("string", JavaTypeToJsonSchemaMapper.getJsonSchemaType(Void.class));
    assertEquals("string", JavaTypeToJsonSchemaMapper.getJsonSchemaType(java.util.Date.class));
  }

  @Test
  void enumConstants_shouldExposeJsonSchemaType() {
    assertEquals("string", JavaTypeToJsonSchemaMapper.STRING.getJsonSchemaType());
    assertEquals("object", JavaTypeToJsonSchemaMapper.OBJECT.getJsonSchemaType());
  }
}
