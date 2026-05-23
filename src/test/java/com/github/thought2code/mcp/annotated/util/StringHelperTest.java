package com.github.thought2code.mcp.annotated.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StringHelperTest {

  @Test
  void constructor_shouldNotBeInstantiable() {
    assertThrows(UnsupportedOperationException.class, StringHelper::new);
  }

  @Test
  void isBlank_shouldReturnTrueForNullEmptyAndWhitespace() {
    assertTrue(StringHelper.isBlank(null));
    assertTrue(StringHelper.isBlank(StringHelper.EMPTY));
    assertTrue(StringHelper.isBlank(StringHelper.SPACE));
    assertTrue(StringHelper.isBlank("   \t\n"));
  }

  @Test
  void isBlank_shouldReturnFalseForNonBlankText() {
    assertFalse(StringHelper.isBlank("test"));
    assertFalse(StringHelper.isBlank("  x  "));
  }

  @Test
  void defaultIfBlank_shouldReturnDefaultWhenInputIsBlank() {
    assertEquals("default", StringHelper.defaultIfBlank(null, "default"));
    assertEquals("default", StringHelper.defaultIfBlank(StringHelper.EMPTY, "default"));
    assertEquals("default", StringHelper.defaultIfBlank(StringHelper.SPACE, "default"));
  }

  @Test
  void defaultIfBlank_shouldReturnInputWhenNotBlank() {
    assertEquals("test", StringHelper.defaultIfBlank("test", "default"));
  }
}
