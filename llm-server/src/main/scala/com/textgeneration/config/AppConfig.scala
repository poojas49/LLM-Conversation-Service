package com.textgeneration.config

import com.typesafe.config.{Config, ConfigFactory}

class AppConfig {
  private val config: Config = ConfigFactory.load()

  object Server {
    private val serverConfig = config.getConfig("server")
    val host: String = serverConfig.getString("host")
    val port: Int = serverConfig.getInt("port")
  }

  object AWS {
    private val awsConfig = config.getConfig("aws.api-gateway")
    val apiGatewayUrl: String = awsConfig.getString("url")
  }

  object Cache {
    private val cacheConfig = config.getConfig("cache")
    val maxSize: Int = cacheConfig.getInt("max-size")
    val ttlSeconds: Long = cacheConfig.getLong("ttl-seconds")
  }

  object TextGeneration {
    private val textGenConfig = config.getConfig("text-generation")
    val defaultMaxLength: Int = textGenConfig.getInt("default-max-length")
    val defaultTemperature: Double = textGenConfig.getDouble("default-temperature")
  }
}

object AppConfig {
  val config = new AppConfig()
}