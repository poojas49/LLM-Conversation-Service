package com.example.routes

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import com.example.domain._
import com.example.domain.JsonFormats._
import com.example.services.LlmService
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import scala.concurrent.ExecutionContext

class LlmRoutes(llmService: LlmService)(implicit ec: ExecutionContext) {
  val routes = {
    pathPrefix("api" / "v1") {
      path("generate") {
        post {
          entity(as[LlmRequest]) { request =>
            onComplete(llmService.processQuery(request)) {
              case scala.util.Success(response) =>
                complete(LlmResponse(response))
              case scala.util.Failure(ex) =>
                complete((StatusCodes.InternalServerError, ErrorResponse(ex.getMessage)))
            }
          }
        }
      }
    }
  }
}