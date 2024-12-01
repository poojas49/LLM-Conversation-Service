package config

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._

class AppConfigSpec extends AnyFlatSpec with Matchers {
  // Test config
  private val testConfig = ConfigFactory.parseString(
    """
      |ollama {
      |  host = "http://localhost:11434"
      |  model = "test-model"
      |  request-timeout-seconds = 30
      |}
      |
      |service {
      |  host = "localhost"
      |  port = 8080
      |}
      |
      |conversation {
      |  max-turns = 5
      |  timeout-minutes = 10
      |}
      |
      |cloud-service {
      |  temperature = 0.7
      |  max-tokens = 150
      |  request-timeout-seconds = 30
      |}
      |
      |server {
      |  host = "0.0.0.0"
      |  port = 8081
      |  termination-timeout-seconds = 10
      |}
    """.stripMargin)

  "AppConfig" should "load Ollama configuration correctly" in {
    val config = AppConfig.Ollama
    config.host should not be empty
    config.model should not be empty
    config.requestTimeoutSeconds should be > 0
  }

  it should "load Service configuration correctly" in {
    val config = AppConfig.Service
    config.host should not be empty
    config.port should be > 0
  }

  it should "load Conversation configuration correctly" in {
    val config = AppConfig.Conversation
    config.maxTurns should be > 0
    config.timeoutMinutes should be > 0
  }

  it should "load CloudService configuration correctly" in {
    val config = AppConfig.CloudService
    config.temperature should be >= 0.0
    config.temperature should be <= 1.0
    config.maxTokens should be > 0
    config.requestTimeoutSeconds should be > 0
  }

  it should "load Server configuration correctly" in {
    val config = AppConfig.Server
    config.host should not be empty
    config.port should be > 0
    config.terminationTimeoutSeconds should be > 0
  }
}