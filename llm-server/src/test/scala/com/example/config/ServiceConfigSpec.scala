package com.example.config

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.typesafe.config.ConfigFactory

class ServiceConfigSpec extends AnyFlatSpec with Matchers {
  "ServiceConfig" should "load valid configuration" in {
    val testConfig = """
                       |service.api-gateway-url = "http://test.example.com"
                       |http.host = "localhost"
                       |http.port = 8080
                       |llm.default-temperature = 0.7
                       |llm.default-max-tokens = 150
                       |llm.headers.content-type = "application/x-protobuf"
    """.stripMargin

    val config = ConfigFactory.parseString(testConfig)
    val serviceConfig = ServiceConfig.load()

    serviceConfig.apiGatewayUrl should not be empty
    serviceConfig.http.port should be >= 0
    serviceConfig.http.port should be <= 65535
    serviceConfig.llm.defaultTemperature should be >= 0.0
    serviceConfig.llm.defaultTemperature should be <= 1.0
  }
}