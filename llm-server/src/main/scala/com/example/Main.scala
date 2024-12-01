package com.example

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import com.example.config.ServiceConfig
import com.example.routes.LlmRoutes
import com.example.services.{LlmService, ProtobufService}
import org.slf4j.LoggerFactory
import scala.util.{Success, Failure}
import scala.io.StdIn
import scala.concurrent.duration._

object Main extends App {
  private val logger = LoggerFactory.getLogger(getClass)

  logger.info("Starting LLM REST service application")
  logger.debug("Initializing actor system and execution context")

  implicit val system = {
    logger.trace("Creating ActorSystem with empty behavior")
    ActorSystem(Behaviors.empty, "llm-rest-service")
  }
  implicit val executionContext = {
    logger.trace("Getting execution context from actor system")
    system.executionContext
  }

  try {
    logger.debug("Loading service configuration")
    val config = ServiceConfig.load()
    logger.trace(s"Configuration loaded successfully - HTTP host: ${config.http.host}, port: ${config.http.port}")

    logger.debug("Initializing service components")
    val protobufService = {
      logger.trace("Creating new ProtobufService instance")
      new ProtobufService()
    }

    val llmService = {
      logger.trace("Creating new LlmService instance with config and protobuf service")
      new LlmService(config, protobufService)
    }

    val routes = {
      logger.trace("Creating new LlmRoutes instance with llm service")
      new LlmRoutes(llmService)
    }

    logger.info(s"Starting HTTP server at ${config.http.host}:${config.http.port}")
    val bindingFuture = Http()(system)
      .newServerAt(config.http.host, config.http.port)
      .bind(routes.routes)
      .andThen {
        case Success(binding) =>
          logger.info(s"Server successfully bound to ${config.http.host}:${config.http.port}")
          logger.debug(s"Server binding details: ${binding.localAddress}")
        case Failure(ex) =>
          logger.error(s"Failed to bind server to ${config.http.host}:${config.http.port}", ex)
          throw ex
      }(executionContext)

    logger.info("Server is ready to accept requests")
    logger.debug("Waiting for shutdown signal (RETURN key)")

    // Monitor system health periodically
    system.scheduler.scheduleAtFixedRate(30.seconds, 30.seconds)(() => {
      val runtime = Runtime.getRuntime
      val usedMemoryMB = (runtime.totalMemory - runtime.freeMemory) / 1024 / 1024
      logger.debug(s"System health check - Used memory: $usedMemoryMB MB")
      if (usedMemoryMB > 1024) { // 1GB threshold
        logger.warn(s"High memory usage detected: $usedMemoryMB MB")
      }
    })(executionContext)

    StdIn.readLine()

    logger.info("Shutdown signal received, initiating graceful shutdown")
    logger.debug("Unbinding server and terminating actor system")

    bindingFuture
      .flatMap { binding =>
        logger.trace("Unbinding server connections")
        binding.unbind()
      }
      .onComplete {
        case Success(_) =>
          logger.debug("Server unbound successfully, terminating actor system")
          system.terminate()
          logger.info("Server shutdown completed successfully")

        case Failure(ex) =>
          logger.error("Error during server shutdown", ex)
          system.terminate()
      }

  } catch {
    case ex: Exception =>
      logger.error("Fatal error during server startup", ex)
      system.terminate()
      throw ex
  }
}