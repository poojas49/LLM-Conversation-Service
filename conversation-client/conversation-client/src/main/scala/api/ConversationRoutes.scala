package api

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import services.ConversationalAgent
import models.{ConversationRequest, ConversationResponse}
import models.ApiModels._ // Import all implicit formats
import play.api.libs.json._
import scala.util.{Success, Failure, Try}
import com.typesafe.scalalogging.LazyLogging

/**
 * API Routes
 *
 * Design Principles:
 * - Clean separation of routing and business logic
 * - Comprehensive error handling
 * - Detailed logging for debugging
 * - Performance monitoring
 */
class ConversationRoutes(conversationalAgent: ConversationalAgent) extends LazyLogging {
  logger.info("Initializing ConversationRoutes")

  val routes: Route = {
    path("conversation") {
      post {
        entity(as[String]) { body =>
          logger.debug(s"Received conversation request with body size: ${body.length} bytes")

          val startTime = System.currentTimeMillis()

          Try {
            logger.debug("Parsing request body as JSON")
            val request = Json.parse(body).as[ConversationRequest]
            logger.info(s"Processing conversation request with initial query: ${request.initialQuery.take(100)}${if (request.initialQuery.length > 100) "..." else ""}")

            // Log output file information if present
            request.outputFile.foreach { filename =>
              logger.debug(s"CSV output will be saved to: $filename")
            }

            // Run conversation
            logger.debug("Starting conversation processing")
            val conversation = conversationalAgent.runConversation(request.initialQuery)
            logger.info(s"Conversation completed with ${conversation.length} turns")

            // Save to CSV if filename provided
            request.outputFile.foreach { filename =>
              logger.info(s"Saving conversation to CSV file: $filename")
              try {
                conversationalAgent.saveConversationToCSV(conversation, filename)
                logger.debug(s"Successfully saved conversation to CSV: $filename")
              } catch {
                case e: Exception =>
                  logger.error(s"Failed to save conversation to CSV file: $filename", e)
                  throw e
              }
            }

            // Create response
            logger.debug("Preparing conversation response")
            val totalProcessingTime = conversation.map(_.processingTimeMs).sum
            val averageProcessingTime = totalProcessingTime / conversation.length

            val response = ConversationResponse(
              turns = conversation.length,
              averageProcessingTimeMs = averageProcessingTime,
              conversation = conversation
            )

            logger.info(s"Request processed successfully in ${System.currentTimeMillis() - startTime}ms " +
              s"(${conversation.length} turns, avg ${averageProcessingTime}ms per turn)")

            complete(StatusCodes.OK -> Json.toJson(response).toString)
          } match {
            case Success(result) => result
            case Failure(e: JsResultException) =>
              logger.error("JSON parsing error in request body", e)
              logger.debug(s"Problematic JSON body: $body")
              complete(StatusCodes.BadRequest -> s"Invalid request format: ${e.getMessage}")
            case Failure(e) =>
              val errorMessage = s"Error processing conversation request: ${e.getMessage}"
              logger.error(errorMessage, e)
              complete(StatusCodes.InternalServerError -> errorMessage)
          }
        }
      }
    }
  }
}