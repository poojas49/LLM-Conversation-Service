package com.example

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import com.example.config.ServiceConfig
import com.example.routes.LlmRoutes
import com.example.services.{LlmService, ProtobufService}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Failure}
import scala.io.StdIn

object Main extends App {
  implicit val system = ActorSystem(Behaviors.empty, "llm-rest-service")
  implicit val executionContext = system.executionContext

  val config = ServiceConfig.load()
  val protobufService = new ProtobufService()
  val llmService = new LlmService(config, protobufService)
  val routes = new LlmRoutes(llmService)

  val bindingFuture = Http()(system)
    .newServerAt(config.http.host, config.http.port)
    .bind(routes.routes)

  println(s"Server now online at http://${config.http.host}:${config.http.port}/\nPress RETURN to stop...")

  StdIn.readLine()

  bindingFuture
    .flatMap(_.unbind())
    .onComplete { _ =>
      system.terminate()
      println("Server stopped")
    }
}