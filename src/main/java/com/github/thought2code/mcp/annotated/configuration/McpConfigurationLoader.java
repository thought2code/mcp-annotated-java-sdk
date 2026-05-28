package com.github.thought2code.mcp.annotated.configuration;

import com.github.thought2code.mcp.annotated.enums.McpServerError;
import com.github.thought2code.mcp.annotated.exception.McpServerConfigurationException;
import com.github.thought2code.mcp.annotated.util.JacksonHelper;
import com.github.thought2code.mcp.annotated.util.StringHelper;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
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
    File file = getConfigFilePath(configFileName).toFile();
    McpServerConfiguration configuration =
        JacksonHelper.fromYaml(file, McpServerConfiguration.class);
    log.info("Configuration loaded successfully from file: {}", configFileName);

    final String profile = configuration.profile();
    if (StringHelper.isBlank(profile)) {
      log.info("No profile specified in configuration file: {}", configFileName);
    } else {
      final String profileConfigFileName = configFileName.replace(".yml", "-" + profile + ".yml");
      File profileConfigFile = getConfigFilePath(profileConfigFileName).toFile();
      configuration =
          JacksonHelper.mergeYaml(configuration, profileConfigFile, McpServerConfiguration.class);
      log.info("Profile configuration merged successfully from file: {}", profileConfigFileName);
    }

    McpServerConfiguration mergedConfig =
        McpConfigurationSupport.finalizeMerged(Objects.requireNonNull(configuration), profile);
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
        throw new McpServerConfigurationException(
            McpServerError.CONFIG_FILE_NOT_FOUND.withDetail(fileName));
      }
      return Paths.get(configFileUrl.toURI());
    } catch (URISyntaxException e) {
      // should never happen
      throw new McpServerConfigurationException(
          McpServerError.INVALID_CONFIG_FILE.withDetail(fileName), e);
    }
  }
}
