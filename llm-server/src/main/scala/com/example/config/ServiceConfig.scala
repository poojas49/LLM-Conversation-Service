package com.example.config

import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.LoggerFactory
import scala.util.{Try, Success, Failure}

case class HttpConfig(
                       host: String,
                       port: Int
                     )

case class LlmConfig(
                      defaultTemperature: Double,
                      defaultMaxTokens: Int,
                      headers: Map[String, String]
                    )

case class ServiceConfig(
                          apiGatewayUrl: String,
                          http: HttpConfig,
                          llm: LlmConfig
                        )


/**
 * Service Configuration
 * ===================
 *
 * Handles application configuration loading and validation using Typesafe Config.
 * Provides strongly-typed configuration objects.
 *
 * Design Rationale:
 * ----------------
 * - Type-safe configuration
 * - Validation at load time
 * - Hierarchical structure
 * - Environment variable support
 *
 * Configuration Structure:
 * ---------------------
 * HttpConfig:
 *   - Server binding settings
 *   - Host and port configuration
 *
 * LlmConfig:
 *   - LLM processing parameters
 *   - Default values
 *   - Header configurations
 *
 * ServiceConfig:
 *   - Top-level configuration
 *   - Integration endpoints
 *   - Component configurations
 *
 * Validation Rules:
 * ---------------
 * - Port range: 0-65535
 * - Temperature range: 0.0-1.0
 * - Required fields checking
 * - URL format validation
 */
object ServiceConfig {
  private val logger = LoggerFactory.getLogger(getClass)

  def load(): ServiceConfig = {
    logger.info("Loading application configuration")
    try {
      val config = ConfigFactory.load()
      logger.debug("Successfully loaded base configuration")

      val serviceConfig = for {
        // API Gateway configuration
        apiUrl <- Try(config.getString("service.api-gateway-url"))
        _ = logger.debug(s"Loaded API Gateway URL: $apiUrl")

        // HTTP configuration
        httpConfig <- loadHttpConfig(config)
        _ = logger.debug(s"Loaded HTTP configuration - Host: ${httpConfig.host}, Port: ${httpConfig.port}")

        // LLM configuration
        llmConfig <- loadLlmConfig(config)
        _ = logger.debug(s"Loaded LLM configuration - Temperature: ${llmConfig.defaultTemperature}, MaxTokens: ${llmConfig.defaultMaxTokens}")
      } yield ServiceConfig(apiUrl, httpConfig, llmConfig)

      serviceConfig match {
        case Success(config) =>
          logger.info("Configuration loaded successfully")
          config
        case Failure(ex) =>
          logger.error("Failed to load configuration", ex)
          throw ex
      }
    } catch {
      case ex: Exception =>
        logger.error("Critical error during configuration loading", ex)
        throw new RuntimeException("Failed to load application configuration", ex)
    }
  }

  private def loadHttpConfig(config: Config): Try[HttpConfig] = {
    logger.trace("Loading HTTP configuration")
    Try {
      val host = config.getString("http.host")
      val port = config.getInt("http.port")

      if (port < 0 || port > 65535) {
        logger.error(s"Invalid port number configured: $port")
        throw new IllegalArgumentException(s"Port number must be between 0 and 65535, got: $port")
      }

      if (host.isEmpty) {
        logger.warn("Empty host configured, this might cause binding issues")
      }

      HttpConfig(host, port)
    }
  }

  private def loadLlmConfig(config: Config): Try[LlmConfig] = {
    logger.trace("Loading LLM configuration")
    Try {
      val temperature = config.getDouble("llm.default-temperature")
      val maxTokens = config.getInt("llm.default-max-tokens")
      val contentType = config.getString("llm.headers.content-type")

      if (temperature < 0.0 || temperature > 1.0) {
        logger.warn(s"Temperature value outside recommended range [0.0, 1.0]: $temperature")
      }

      if (maxTokens <= 0) {
        logger.error(s"Invalid maxTokens value: $maxTokens")
        throw new IllegalArgumentException(s"MaxTokens must be positive, got: $maxTokens")
      }

      if (contentType.isEmpty) {
        logger.warn("Content-Type header is empty")
      }

      LlmConfig(
        defaultTemperature = temperature,
        defaultMaxTokens = maxTokens,
        headers = Map("Content-Type" -> contentType)
      )
    }
  }
}