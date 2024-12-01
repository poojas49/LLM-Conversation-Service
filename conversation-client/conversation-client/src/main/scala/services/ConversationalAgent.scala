package services

import com.typesafe.scalalogging.LazyLogging
import io.github.ollama4j.{OllamaAPI, models => ollamaModels, utils => ollamaUtils}
import models.{ConversationTurn, LLMRequest, LLMResponse}
import play.api.libs.json._
import sttp.client3._
import sttp.client3.playJson._
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Try, Success, Failure}
import java.time.LocalDateTime
import java.nio.file.{Paths, Files}
import java.nio.charset.StandardCharsets
import config.AppConfig

class ConversationalAgent extends LazyLogging {
  private val backend = HttpURLConnectionBackend()
  private val ollamaAPI = new OllamaAPI(AppConfig.Ollama.host)
  ollamaAPI.setRequestTimeoutSeconds(AppConfig.Ollama.requestTimeoutSeconds)

  def generateCloudResponse(query: String): Future[String] = {
    val request = basicRequest
      .post(uri"http://${AppConfig.Service.host}:${AppConfig.Service.port}/api/v1/generate")
      .header("Content-Type", "application/json")
      .body(
        Json.obj(
          "inputText" -> query,
          "temperature" -> 0.7,
          "maxTokens" -> 150
        ).toString()
      )

    Future {
      backend.send(request).body match {
        case Right(jsonString) =>
          try {
            val json = Json.parse(jsonString)
            (json \ "response").as[String]
          } catch {
            case e: Exception =>
              logger.error(s"Failed to parse response: $jsonString", e)
              throw e
          }
        case Left(error) =>
          logger.error(s"Cloud service error: $error")
          throw new Exception(s"Cloud service error: $error")
      }
    }
  }

  // Rest of the code remains the same
  def generateOllamaResponse(previousResponse: String): String = {
    val prompt = s"how can you respond to the statement: $previousResponse"
    Try {
      val result = ollamaAPI.generate(
        AppConfig.Ollama.model,
        prompt,
        false,
        new ollamaUtils.Options(new java.util.HashMap[String, Object]())
      )
      result.getResponse
    } match {
      case Success(response) => response
      case Failure(e) =>
        logger.error(s"Ollama error: ${e.getMessage}")
        throw new Exception(s"Ollama error: ${e.getMessage}")
    }
  }

  def runConversation(initialQuery: String): Seq[ConversationTurn] = {
    var conversation = Seq.empty[ConversationTurn]
    var currentQuery = initialQuery
    val startTime = LocalDateTime.now()

    while (
      conversation.length < AppConfig.Conversation.maxTurns &&
        startTime.plusMinutes(AppConfig.Conversation.timeoutMinutes).isAfter(LocalDateTime.now())
    ) {
      val turnStartTime = System.currentTimeMillis()

      try {
        // Get cloud response
        val cloudResponse = Await.result(generateCloudResponse(currentQuery), 30.seconds)
        logger.info(s"Cloud Response: $cloudResponse")

        // Generate next query using Ollama
        val ollamaResponse = generateOllamaResponse(cloudResponse)
        logger.info(s"Ollama Response: $ollamaResponse")

        // Record the turn
        conversation = conversation :+ ConversationTurn(
          LocalDateTime.now(),
          currentQuery,
          cloudResponse,
          ollamaResponse,
          System.currentTimeMillis() - turnStartTime
        )

        // Set up next turn
        currentQuery = ollamaResponse
      } catch {
        case e: Exception =>
          logger.error(s"Error during conversation turn: ${e.getMessage}")
          throw e
      }
    }

    conversation
  }

  def saveConversationToCSV(conversation: Seq[ConversationTurn], filepath: String): Unit = {
    val header = "timestamp,query,cloud_response,ollama_response,processing_time_ms\n"
    val rows = conversation.map { turn =>
      s"${turn.timestamp},${escape(turn.query)},${escape(turn.cloudResponse)},${escape(turn.ollamaResponse)},${turn.processingTimeMs}"
    }.mkString("\n")

    Files.write(
      Paths.get(filepath),
      (header + rows).getBytes(StandardCharsets.UTF_8)
    )
    logger.info(s"Conversation saved to: $filepath")
  }

  private def escape(s: String): String = {
    "\"" + s.replace("\"", "\"\"") + "\""
  }
}