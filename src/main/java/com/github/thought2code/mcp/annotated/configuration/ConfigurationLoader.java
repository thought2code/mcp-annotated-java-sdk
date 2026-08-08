package com.github.thought2code.mcp.annotated.configuration;

import com.github.thought2code.mcp.annotated.enums.McpServerError;
import com.github.thought2code.mcp.annotated.exception.McpServerConfigurationException;
import com.github.thought2code.mcp.annotated.util.JacksonHelper;
import com.github.thought2code.mcp.annotated.util.StringHelper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads and merges {@link ServerConfiguration} from YAML files on the classpath.
 *
 * <p>When {@code profile} is set in the base file, a companion {@code mcp-server-{profile}.yml} is
 * merged on top using Jackson merge semantics.
 *
 * @param configFileName YAML file name (for example {@code mcp-server.yml})
 * @see <a href="https://thought2code.github.io/mcp-annotated-java-sdk/guides/getting-started/">MCP
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
    ServerConfiguration configuration = loadYaml(configFileName);
    log.info("Configuration loaded successfully from file: {}", configFileName);

    final String profile = configuration.profile();
    if (StringHelper.isBlank(profile)) {
      log.info("No profile specified in configuration file: {}", configFileName);
    } else {
      final String profileConfigFileName = configFileName.replace(".yml", "-" + profile + ".yml");
      configuration = mergeYaml(configuration, profileConfigFileName);
      log.info("Profile configuration merged successfully from file: {}", profileConfigFileName);
    }

    ServerConfiguration mergedConfig =
        ConfigurationSupport.finalizeMerged(Objects.requireNonNull(configuration), profile);
    ConfigurationChecker.check(mergedConfig);
    return mergedConfig;
  }

  /**
   * Loads the configuration file from the classpath.
   *
   * @param fileName the name of the configuration file
   * @return the loaded server configuration
   * @throws McpServerConfigurationException if the configuration file cannot be found or read
   */
  private ServerConfiguration loadYaml(String fileName) {
    try (InputStream inputStream = getConfigInputStream(fileName)) {
      return JacksonHelper.fromYaml(inputStream, fileName, ServerConfiguration.class);
    } catch (IOException e) {
      throw new McpServerConfigurationException(
          McpServerError.YAML_READ_ERROR.withDetail(fileName), e);
    }
  }

  /**
   * Merges the profile configuration file from the classpath.
   *
   * @param configuration the base configuration
   * @param profileConfigFileName the name of the profile configuration file
   * @return the merged server configuration
   * @throws McpServerConfigurationException if the profile configuration file cannot be found or
   *     read
   */
  private ServerConfiguration mergeYaml(
      ServerConfiguration configuration, String profileConfigFileName) {
    try (InputStream inputStream = getConfigInputStream(profileConfigFileName)) {
      return JacksonHelper.mergeYaml(
          configuration, inputStream, profileConfigFileName, ServerConfiguration.class);
    } catch (IOException e) {
      throw new McpServerConfigurationException(
          McpServerError.YAML_READ_ERROR.withDetail(profileConfigFileName), e);
    }
  }

  /**
   * Returns the classpath input stream of the configuration file.
   *
   * @param fileName the name of the configuration file
   * @return the input stream of the configuration file
   * @throws McpServerConfigurationException if the configuration file cannot be found
   */
  private InputStream getConfigInputStream(String fileName) {
    ClassLoader classLoader = ConfigurationLoader.class.getClassLoader();
    InputStream inputStream = classLoader.getResourceAsStream(fileName);
    if (inputStream == null) {
      throw new McpServerConfigurationException(
          McpServerError.CONFIG_FILE_NOT_FOUND.withDetail(fileName));
    }
    return inputStream;
  }
}
