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
 * Loads and merges {@link ServerConfiguration} from YAML files on the classpath or filesystem.
 *
 * <p>When {@code profile} is set in the base file, a companion {@code mcp-server-{profile}.yml} is
 * merged on top using Jackson merge semantics.
 *
 * @param configFileName YAML file name (for example {@code mcp-server.yml})
 * @see <a href="https://thought2code.github.io/mcp-annotated-java-sdk/getting-started">MCP
 *     Annotated Java SDK Documentation</a>
 * @author codeboyzhou
 */
public record ConfigurationLoader(String configFileName) {

  /** The logger instance for this class. */
  private static final Logger log = LoggerFactory.getLogger(ConfigurationLoader.class);

  /**
   * Loads the MCP server configuration from the specified YAML file.
   *
   * @return the loaded MCP server configuration
   * @throws McpServerConfigurationException if the configuration file cannot be loaded
   */
  public ServerConfiguration loadConfig() {
    File file = getConfigFilePath(configFileName).toFile();
    ServerConfiguration configuration = JacksonHelper.fromYaml(file, ServerConfiguration.class);
    log.info("Configuration loaded successfully from file: {}", configFileName);

    final String profile = configuration.profile();
    if (StringHelper.isBlank(profile)) {
      log.info("No profile specified in configuration file: {}", configFileName);
    } else {
      final String profileConfigFileName = configFileName.replace(".yml", "-" + profile + ".yml");
      File profileConfigFile = getConfigFilePath(profileConfigFileName).toFile();
      configuration =
          JacksonHelper.mergeYaml(configuration, profileConfigFile, ServerConfiguration.class);
      log.info("Profile configuration merged successfully from file: {}", profileConfigFileName);
    }

    ServerConfiguration mergedConfig =
        ConfigurationSupport.finalizeMerged(Objects.requireNonNull(configuration), profile);
    ConfigurationChecker.check(mergedConfig);
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
      ClassLoader classLoader = ConfigurationLoader.class.getClassLoader();
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
