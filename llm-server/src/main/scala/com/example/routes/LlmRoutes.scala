package com.example.routes

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import com.example.domain._
import com.example.domain.JsonFormats._
import com.example.services.LlmService
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import scala.concurrent.ExecutionContext
import org.slf4j.LoggerFactory

/**
 * HTTP Routes Definition
 * =====================
 *
 * Defines the REST API endpoints for the LLM service, handling HTTP routing
 * and request/response mapping.
 *
 * Design Rationale:
 * ----------------
 * - Clean REST API structure
 * - Versioned endpoints
 * - Consistent error handling
 * - Request/response logging
 *
 * API Endpoints:
 * ------------
 * POST /api/v1/generate
 *   - Input: LlmRequest
 *   - Output: LlmResponse
 *   - Error: ErrorResponse
 *
 * Error Handling:
 * -------------
 * - HTTP 500 for internal errors
 * - Structured error responses
 * - Error logging with stack traces
 */
class LlmRoutes(llmService: LlmService)(implicit ec: ExecutionContext) {
  private val logger = LoggerFactory.getLogger(getClass)

  val routes = {
    pathPrefix("api" / "v1") {
      path("generate") {
        post {
          entity(as[LlmRequest]) { request =>
            logger.info(s"Received generation request with input length: ${request.inputText.length}")

            onComplete(llmService.processQuery(request)) {
              case scala.util.Success(response) =>
                logger.info(s"Successfully processed request, response length: ${response.length}")
                complete(LlmResponse(response))

              case scala.util.Failure(ex) =>
                logger.error("Failed to process generation request", ex)
                complete((StatusCodes.InternalServerError, ErrorResponse(ex.getMessage)))
            }
          }
        }
      }
    }
  }
}