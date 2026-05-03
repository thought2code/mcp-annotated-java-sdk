package com.github.thought2code.mcp.annotated.server.component;

import com.github.thought2code.mcp.annotated.annotation.McpI18nEnabled;
import com.github.thought2code.mcp.annotated.util.Immutable;
import com.github.thought2code.mcp.annotated.util.StringHelper;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A provider class for managing internationalization (i18n) resource bundles.
 *
 * <p>This class provides static methods for loading and accessing resource bundles to support
 * internationalization in MCP server applications. It uses the {@link McpI18nEnabled} annotation to
 * configure the resource bundle base name.
 *
 * <p>Each provider instance maintains a {@link ResourceBundle} loaded from the specified base name
 * using the default locale.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Loads resource bundles based on {@link McpI18nEnabled} annotation configuration
 *   <li>Supports default locale for resource bundle resolution
 * </ul>
 *
 * @author codeboyzhou
 * @see ResourceBundle
 * @see McpI18nEnabled
 * @see Locale
 */
public final class ResourceBundleProvider {

  public static final Logger log = LoggerFactory.getLogger(ResourceBundleProvider.class);

  /** The {@link ResourceBundle} instance loaded for this application context. */
  private final Immutable<ResourceBundle> bundle;

  private ResourceBundleProvider(ResourceBundle bundle) {
    this.bundle = bundle == null ? null : Immutable.of(bundle);
  }

  /**
   * Loads a resource bundle based on the {@link McpI18nEnabled} annotation on the main class.
   *
   * <p>This method checks if the main class is annotated with {@code @McpI18nEnabled}. If the
   * annotation is present, it loads a resource bundle using the base name specified in the
   * annotation's {@code resourceBundleBaseName} attribute. The resource bundle is loaded using the
   * default locale.
   *
   * <p>If the annotation is not present, the method logs an info message and returns without
   * loading any resource bundle, effectively disabling i18n support.
   *
   * @param mainClass the main application class to check for the McpI18nEnabled annotation
   * @throws IllegalArgumentException if the resourceBundleBaseName is blank
   * @throws MissingResourceException if no resource bundle is found for the specified base name
   * @see McpI18nEnabled
   * @see ResourceBundle#getBundle(String, Locale)
   * @see Locale#getDefault()
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
   * Retrieves the string with the specified i18n key using the resource bundle, or returns the
   * default value if the key is not found in the bundle.
   *
   * @param i18nKey the i18n key of the attribute to localize
   * @param defaultValue the default value to return if the i18n key is not found in the bundle
   * @return the localized value of the attribute, or the default value if the i18n key is not found
   */
  public String getString(String i18nKey, String defaultValue) {
    if (bundle != null && bundle.get().containsKey(i18nKey)) {
      return bundle.get().getString(i18nKey);
    }
    return StringHelper.defaultIfBlank(i18nKey, defaultValue);
  }
}
