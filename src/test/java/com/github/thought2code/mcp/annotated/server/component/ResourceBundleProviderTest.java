package com.github.thought2code.mcp.annotated.server.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.thought2code.mcp.annotated.annotation.McpI18nEnabled;
import org.junit.jupiter.api.Test;

class ResourceBundleProviderTest {

  @Test
  void loadResourceBundle_shouldReturnEmptyProviderWhenI18nDisabled() {
    ResourceBundleProvider provider =
        ResourceBundleProvider.loadResourceBundle(PlainApplication.class);
    assertEquals("missing.key", provider.getString("missing.key", "fallback"));
    assertEquals("literal", provider.getString("literal", "default"));
  }

  @Test
  void loadResourceBundle_shouldResolveLocalizedValuesWhenEnabled() {
    ResourceBundleProvider provider =
        ResourceBundleProvider.loadResourceBundle(I18nApplication.class);
    assertEquals(
        "localized_resource1_name", provider.getString("resource1_name", "resource1_name"));
    assertEquals("missing.key", provider.getString("missing.key", "fallback"));
  }

  @Test
  void loadResourceBundle_shouldRejectBlankBaseName() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> ResourceBundleProvider.loadResourceBundle(InvalidI18nApplication.class));
    assertTrue(exception.getMessage().contains("resourceBundleBaseName"));
  }

  static class PlainApplication {}

  @McpI18nEnabled(resourceBundleBaseName = "i18n.test-messages")
  static class I18nApplication {}

  @McpI18nEnabled(resourceBundleBaseName = " ")
  static class InvalidI18nApplication {}
}
