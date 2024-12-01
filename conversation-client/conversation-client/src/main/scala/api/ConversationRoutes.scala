package api

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import services.ConversationalAgent
import models.{ConversationRequest, ConversationResponse}
import models.ApiModels._ // Import all implicit formats
import play.api.libs.json._
import scala.util.{Success, Failure}
import com.typesafe.scalalogging.LazyLogging

class ConversationRoutes(conversationalAgent: ConversationalAgent) extends LazyLogging {

  val routes: Route = {
    path("conversation") {
      post {
        entity(as[String]) { body =>
          try {
            val request = Json.parse(body).as[ConversationRequest]

            // Run conversation
            val conversation = conversationalAgent.runConversation(request.initialQuery)

            // Save to CSV if filename provided
            request.outputFile.foreach { filename =>
              conversationalAgent.saveConversationToCSV(conversation, filename)
            }

            // Create response
            val response = ConversationResponse(
              turns = conversation.length,
              averageProcessingTimeMs = conversation.map(_.processingTimeMs).sum / conversation.length,
              conversation = conversation
            )

            complete(StatusCodes.OK -> Json.toJson(response).toString)
          } catch {
            case e: Exception =>
              logger.error("Error processing conversation request", e)
              complete(StatusCodes.InternalServerError -> s"Error: ${e.getMessage}")
          }
        }
      }
    }
  }
}