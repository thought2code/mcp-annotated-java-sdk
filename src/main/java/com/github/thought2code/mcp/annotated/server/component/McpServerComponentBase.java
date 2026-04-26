package com.github.thought2code.mcp.annotated.server.component;

import com.github.thought2code.mcp.annotated.util.StringHelper;
import java.util.ResourceBundle;

/**
 * Abstract base class that provides common functionality for MCP server components
 * (resource/prompt/tool).
 *
 * <p>This class implements the {@link McpServerComponent} interface and provides shared
 * functionality for all MCP server components, including i18n support and server instance
 * management. It serves as a foundation for concrete component implementations that handle specific
 * types of MCP components.
 *
 * <p>The class provides:
 *
 * <ul>
 *   <li>Access to the MCP synchronous server instance through a supplier
 *   <li>I18n support through resource bundle handling
 *   <li>Attribute localization with fallback to default values
 * </ul>
 *
 * @param <T> the type of component this base class creates (e.g., McpSchema.Resource,
 *     McpSchema.Prompt, McpSchema.Tool)
 * @author codeboyzhou
 * @see McpServerComponent
 * @see ResourceBundle
 */
public abstract class McpServerComponentBase<T> implements McpServerComponent<T> {
  /** Resource bundle for i18n support */
  private final ResourceBundle bundle;

  /** Constructor that initializes the resource bundle for i18n support. */
  public McpServerComponentBase() {
    this.bundle = ResourceBundleProvider.getResourceBundle();
  }

  /**
   * Localizes an attribute with the specified i18n key using the resource bundle, or returns the
   * default value if the key is not found in the bundle.
   *
   * <p>This method attempts to retrieve a localized string from the resource bundle using the
   * provided key. If the key is not found or the bundle is unavailable, it falls back to either the
   * key itself (if it contains a non-blank value) or the specified default value.
   *
   * @param i18nKey the i18n key of the attribute to localize
   * @param defaultValue the default value to return if the i18n key is not found in the bundle
   * @return the localized value of the attribute, or the default value if the i18n key is not found
   * @see ResourceBundle
   * @see StringHelper#defaultIfBlank(String, String)
   */
  protected String localizeAttribute(String i18nKey, String defaultValue) {
    if (bundle != null && bundle.containsKey(i18nKey)) {
      return bundle.getString(i18nKey);
    }
    // If we don't need to localize, just return the literal value or default value
    return StringHelper.defaultIfBlank(i18nKey, defaultValue);
  }
}
