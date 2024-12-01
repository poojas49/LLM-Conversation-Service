package com.example.config

import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._

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

object ServiceConfig {
  def load(): ServiceConfig = {
    val config = ConfigFactory.load()

    ServiceConfig(
      apiGatewayUrl = config.getString("service.api-gateway-url"),
      http = HttpConfig(
        host = config.getString("http.host"),
        port = config.getInt("http.port")
      ),
      llm = LlmConfig(
        defaultTemperature = config.getDouble("llm.default-temperature"),
        defaultMaxTokens = config.getInt("llm.default-max-tokens"),
        headers = Map(
          "Content-Type" -> config.getString("llm.headers.content-type")
        )
      )
    )
  }
}