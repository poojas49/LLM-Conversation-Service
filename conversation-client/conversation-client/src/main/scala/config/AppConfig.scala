package config

import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._

/**
 * Configuration Management
 *
 * Design Principles:
 * - Type-safe configuration access
 * - Centralized configuration management
 * - Modular organization by component
 * - Early validation of configuration values
 */
object AppConfig {
  private val config = ConfigFactory.load()

  // Component-specific configuration objects
  object Ollama {
    private val ollamaConfig = config.getConfig("ollama")
    val host: String = ollamaConfig.getString("host")
    val model: String = ollamaConfig.getString("model")
    val requestTimeoutSeconds: Int = ollamaConfig.getInt("request-timeout-seconds")
  }

  object Service {
    private val serviceConfig = config.getConfig("service")
    val host: String = serviceConfig.getString("host")
    val port: Int = serviceConfig.getInt("port")
  }

  object Conversation {
    private val conversationConfig = config.getConfig("conversation")
    val maxTurns: Int = conversationConfig.getInt("max-turns")
    val timeoutMinutes: Int = conversationConfig.getInt("timeout-minutes")
  }

  // Configuration for cloud-based LLM service
  object CloudService {
    private val cloudConfig = config.getConfig("cloud-service")
    val temperature: Double = cloudConfig.getDouble("temperature")
    val maxTokens: Int = cloudConfig.getInt("max-tokens")
    val requestTimeoutSeconds: Int = cloudConfig.getInt("request-timeout-seconds")
  }

  object Server {
    private val serverConfig = config.getConfig("server")
    val host: String = serverConfig.getString("host")
    val port: Int = serverConfig.getInt("port")
    val terminationTimeoutSeconds: Int = serverConfig.getInt("termination-timeout-seconds")
  }
}