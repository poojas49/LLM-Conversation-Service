package com.textgeneration.controllers

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model._
import com.textgeneration.models._
import com.textgeneration.services.TextGenerationService
import scala.concurrent.ExecutionContext
import scala.util.{Success, Failure}
import org.slf4j.LoggerFactory

class TextGenerationController(service: TextGenerationService)(implicit ec: ExecutionContext)
  extends JsonSupport {

  private val logger = LoggerFactory.getLogger(getClass)

  val routes: Route = {
    pathPrefix("api" / "v1") {
      path("generate") {
        post {
          entity(as[GenerationRequest]) { request =>
            logger.info(s"Received generation request: $request")
            onComplete(service.generateText(request)) {
              case Success(response) =>
                logger.info(s"Successfully generated response for query: ${request.query}")
                complete(StatusCodes.OK -> response)
              case Failure(ex) =>
                logger.error(s"Failed to generate response: ${ex.getMessage}", ex)
                complete(StatusCodes.InternalServerError ->
                  Map("error" -> ex.getMessage))
            }
          }
        }
      } ~
        path("health") {
          get {
            complete(StatusCodes.OK -> Map("status" -> "healthy"))
          }
        }
    }
  }
}