package com.github.thought2code.mcp.annotated.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TypeConverterTest {

  @Test
  void constructor_shouldNotBeInstantiable() {
    assertThrows(UnsupportedOperationException.class, TypeConverter::new);
  }

  @Test
  void convert_shouldReturnDefaultsWhenValueIsNull() {
    assertEquals(StringHelper.EMPTY, TypeConverter.convert(null, String.class));
    assertEquals(0, TypeConverter.convert(null, int.class));
    assertEquals(0, TypeConverter.convert(null, Integer.class));
    assertEquals(0L, TypeConverter.convert(null, long.class));
    assertEquals(0L, TypeConverter.convert(null, Long.class));
    assertEquals(0.0F, TypeConverter.convert(null, float.class));
    assertEquals(0.0F, TypeConverter.convert(null, Float.class));
    assertEquals(0.0, TypeConverter.convert(null, double.class));
    assertEquals(0.0, TypeConverter.convert(null, Double.class));
    assertEquals(0.0, TypeConverter.convert(null, Number.class));
    assertEquals(false, TypeConverter.convert(null, boolean.class));
    assertEquals(false, TypeConverter.convert(null, Boolean.class));
    assertNull(TypeConverter.convert(null, Object.class));
  }

  @Test
  void convert_shouldParsePrimitiveWrappersFromString() {
    assertEquals("test", TypeConverter.convert("test", String.class));
    assertEquals(1, TypeConverter.convert("1", int.class));
    assertEquals(1, TypeConverter.convert("1", Integer.class));
    assertEquals(1L, TypeConverter.convert("1", long.class));
    assertEquals(1L, TypeConverter.convert("1", Long.class));
    assertEquals(1.0F, TypeConverter.convert("1", float.class));
    assertEquals(1.0F, TypeConverter.convert("1", Float.class));
    assertEquals(1.0, TypeConverter.convert("1", double.class));
    assertEquals(1.0, TypeConverter.convert("1", Double.class));
    assertEquals(true, TypeConverter.convert("true", boolean.class));
    assertEquals(true, TypeConverter.convert("true", Boolean.class));
  }

  @Test
  void convert_shouldParseNumberWithIntegerLongAndDoubleSemantics() {
    assertEquals(2147483647, TypeConverter.convert(Integer.MAX_VALUE, Number.class));
    assertEquals(9223372036854775807L, TypeConverter.convert(Long.MAX_VALUE, Number.class));
    assertEquals(1.0, TypeConverter.convert("1.0", Number.class));
    assertEquals(42, TypeConverter.convert("42", Number.class));
  }

  @Test
  void convert_shouldPassThroughUnsupportedTypes() {
    assertEquals("test", TypeConverter.convert("test", Object.class));
    assertEquals(123, TypeConverter.convert(123, Object.class));
  }
}
