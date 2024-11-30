package com.textgeneration

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import com.textgeneration.controllers.TextGenerationController
import com.textgeneration.services.TextGenerationService
import com.textgeneration.clients.ProtoHttpClient
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Failure}
import scala.io.StdIn
import org.slf4j.LoggerFactory
import com.typesafe.config.ConfigFactory

object TextGenerationApp extends App {
  private val logger = LoggerFactory.getLogger(getClass)
  private val config = ConfigFactory.load()

  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "TextGenerationSystem")
  implicit val executionContext: ExecutionContext = system.executionContext

  val host = config.getString("server.host")
  val port = config.getInt("server.port")
  val apiGatewayUrl = config.getString("aws.api-gateway.url")

  val protoClient = new ProtoHttpClient(apiGatewayUrl)
  val textGenerationService = new TextGenerationService(protoClient)
  val controller = new TextGenerationController(textGenerationService)

  val bindingFuture = Http().newServerAt(host, port).bind(controller.routes)

  bindingFuture.onComplete {
    case Success(binding) =>
      val address = binding.localAddress
      logger.info(s"Server online at http://${address.getHostString}:${address.getPort}/")
      logger.info("Press RETURN to stop...")
    case Failure(ex) =>
      logger.error(s"Failed to bind server: ${ex.getMessage}", ex)
      system.terminate()
  }

  StdIn.readLine()

  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => {
      system.terminate()
      logger.info("Server stopped")
    })
}