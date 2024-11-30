package com.textgeneration

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import com.textgeneration.controllers.TextGenerationController
import com.textgeneration.services.TextGenerationService
import com.textgeneration.repositories.CacheRepository
import com.textgeneration.clients.LambdaClient
import com.textgeneration.config.AppConfig

import scala.concurrent.ExecutionContext
import scala.util.{Success, Failure}
import scala.io.StdIn
import org.slf4j.LoggerFactory

object TextGenerationApp extends App {
  private val logger = LoggerFactory.getLogger(getClass)
  private val appConfig = AppConfig.config

  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "TextGenerationSystem")
  implicit val executionContext: ExecutionContext = system.executionContext

  // Initialize components with configuration
  val cacheRepository = new CacheRepository(
    maxSize = appConfig.Cache.maxSize,
    ttlSeconds = appConfig.Cache.ttlSeconds
  )

  val lambdaClient = new LambdaClient(
    apiGatewayUrl = appConfig.AWS.apiGatewayUrl
  )

  val textGenerationService = new TextGenerationService(
    cacheRepository,
    lambdaClient
  )

  val controller = new TextGenerationController(textGenerationService)

  // Start server
  val bindingFuture = Http().newServerAt(
    appConfig.Server.host,
    appConfig.Server.port
  ).bind(controller.routes)

  bindingFuture.onComplete {
    case Success(binding) =>
      val address = binding.localAddress
      logger.info(s"Server online at http://${address.getHostString}:${address.getPort}/")
      logger.info("Press RETURN to stop...")
    case Failure(ex) =>
      logger.error(s"Failed to bind server: ${ex.getMessage}", ex)
      system.terminate()
  }

  // Keep the app running until user presses return
  try {
    StdIn.readLine()
  } finally {
    bindingFuture
      .flatMap(_.unbind())
      .onComplete { _ =>
        logger.info("Server stopped")
        system.terminate()
      }
  }
}