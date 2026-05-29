package com.github.thought2code.mcp.annotated.server.component;

import com.github.thought2code.mcp.annotated.annotation.McpI18nEnabled;
import com.github.thought2code.mcp.annotated.util.StringHelper;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides i18n lookups backed by an immutable map resolved from {@link ResourceBundle}.
 *
 * @author codeboyzhou
 */
public final class ResourceBundleProvider {

  /** Logger for resource bundle loading diagnostics. */
  public static final Logger log = LoggerFactory.getLogger(ResourceBundleProvider.class);

  /** Immutable map of localized values resolved from a resource bundle. */
  private final Map<String, String> localizedValues;

  private ResourceBundleProvider(ResourceBundle bundle) {
    this.localizedValues = bundle == null ? Collections.emptyMap() : toLocalizedValueMap(bundle);
  }

  /**
   * Loads i18n values from the main class configuration.
   *
   * @param mainClass application entry class inspected for {@link McpI18nEnabled}
   * @return a provider with localized values, or an empty provider when i18n is disabled
   * @throws IllegalArgumentException if {@code resourceBundleBaseName} is blank
   * @throws MissingResourceException if the configured bundle cannot be found
   */
  public static ResourceBundleProvider loadResourceBundle(Class<?> mainClass) {
    McpI18nEnabled mcpI18nEnabled = mainClass.getAnnotation(McpI18nEnabled.class);
    if (mcpI18nEnabled == null) {
      log.info("McpI18nEnabled annotation is not present on the main class, skip i18n support.");
      return new ResourceBundleProvider(null);
    }

    final String baseName = mcpI18nEnabled.resourceBundleBaseName();
    if (StringHelper.isBlank(baseName)) {
      throw new IllegalArgumentException("resourceBundleBaseName must not be blank.");
    }

    log.info("Loading resource bundle with base name: {}", baseName);
    ResourceBundle resourceBundle = ResourceBundle.getBundle(baseName, Locale.getDefault());
    log.info("Resource bundle loaded successfully with base name: {}", baseName);
    return new ResourceBundleProvider(resourceBundle);
  }

  /**
   * Returns localized text by key, or falls back to the provided default.
   *
   * @param i18nKey localization key to resolve
   * @param defaultValue value used when the key is missing or blank
   * @return localized text, or the default when no translation exists
   */
  public String getString(String i18nKey, String defaultValue) {
    if (localizedValues.containsKey(i18nKey)) {
      return localizedValues.get(i18nKey);
    }
    return StringHelper.defaultIfBlank(i18nKey, defaultValue);
  }

  /**
   * Converts a {@link ResourceBundle} to an immutable map of localized values.
   *
   * @param bundle the resource bundle to convert
   * @return an immutable map of localized values where keys are i18n keys and values are the
   *     localized text
   */
  private static Map<String, String> toLocalizedValueMap(ResourceBundle bundle) {
    Set<String> keys = bundle.keySet();
    Map<String, String> values = new HashMap<>();
    for (String key : keys) {
      values.put(key, bundle.getString(key));
    }
    return Collections.unmodifiableMap(values);
  }
}
