package com.github.thought2code.mcp.annotated.configuration;

import com.github.thought2code.mcp.annotated.exception.McpServerConfigurationException;
import com.github.thought2code.mcp.annotated.util.JacksonHelper;
import com.github.thought2code.mcp.annotated.util.StringHelper;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This record represents a YAML configuration loader for MCP (Model Context Protocol) server
 * configuration.
 *
 * <p>It loads the server configuration from a specified YAML file. If no file name is provided, the
 * default file name "mcp-server.yml" will be used.
 *
 * @see <a href="https://thought2code.github.io/mcp-annotated-java-sdk/getting-started">MCP
 *     Annotated Java SDK Documentation</a>
 * @author codeboyzhou
 */
public record McpConfigurationLoader(String configFileName) {

  /** The logger instance for this class. */
  private static final Logger log = LoggerFactory.getLogger(McpConfigurationLoader.class);

  /**
   * Loads the MCP server configuration from the specified YAML file.
   *
   * @return the loaded MCP server configuration
   * @throws McpServerConfigurationException if the configuration file cannot be loaded
   */
  public McpServerConfiguration loadConfig() {
    Path configFilePath = getConfigFilePath(configFileName);
    File file = configFilePath.toFile();
    McpServerConfiguration baseConfig = JacksonHelper.fromYaml(file, McpServerConfiguration.class);
    log.info("Configuration loaded successfully from file: {}", configFileName);

    final String profile = baseConfig.profile();
    if (StringHelper.isBlank(profile)) {
      log.info("No profile specified in configuration file: {}", configFileName);
      McpConfigurationChecker.check(baseConfig);
      return baseConfig;
    }

    final String profileConfigFileName = configFileName.replace(".yml", "-" + profile + ".yml");
    Path profileConfigFilePath = getConfigFilePath(profileConfigFileName);
    File profileConfigFile = profileConfigFilePath.toFile();
    McpServerConfiguration profileConfig =
        JacksonHelper.fromYaml(profileConfigFile, McpServerConfiguration.class);
    log.info("Profile configuration loaded successfully from file: {}", profileConfigFileName);

    McpServerConfiguration mergedConfig = McpConfigurationMerger.merge(baseConfig, profileConfig);
    McpConfigurationChecker.check(mergedConfig);
    return mergedConfig;
  }

  /**
   * Returns the file path of the configuration file.
   *
   * @param fileName the name of the configuration file
   * @return the file path of the configuration file
   * @throws McpServerConfigurationException if the configuration file cannot be found
   */
  private Path getConfigFilePath(String fileName) {
    try {
      ClassLoader classLoader = McpConfigurationLoader.class.getClassLoader();
      URL configFileUrl = classLoader.getResource(fileName);
      if (configFileUrl == null) {
        throw new McpServerConfigurationException("Configuration file not found: " + fileName);
      }
      return Paths.get(configFileUrl.toURI());
    } catch (URISyntaxException e) {
      // should never happen
      throw new McpServerConfigurationException("Invalid configuration file: " + fileName, e);
    }
  }
}
