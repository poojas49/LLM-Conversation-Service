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
  logger.info("Initializing ConversationalAgent...")

  private val backend = {
    logger.debug("Creating HTTP backend...")
    HttpURLConnectionBackend()
  }

  private val ollamaAPI = {
    logger.debug(s"Initializing Ollama API with host: ${AppConfig.Ollama.host}")
    val api = new OllamaAPI(AppConfig.Ollama.host)
    api.setRequestTimeoutSeconds(AppConfig.Ollama.requestTimeoutSeconds)
    logger.debug(s"Ollama API timeout set to ${AppConfig.Ollama.requestTimeoutSeconds} seconds")
    api
  }

  def generateCloudResponse(query: String): Future[String] = {
    logger.debug(s"Generating cloud response for query: ${query.take(100)}${if (query.length > 100) "..." else ""}")

    val request = basicRequest
      .post(uri"http://${AppConfig.Service.host}:${AppConfig.Service.port}/api/v1/generate")
      .header("Content-Type", "application/json")
      .body(
        Json.obj(
          "inputText" -> query,
          "temperature" -> AppConfig.CloudService.temperature,
          "maxTokens" -> AppConfig.CloudService.maxTokens
        ).toString()
      )

    logger.debug(s"Sending request to cloud service at ${AppConfig.Service.host}:${AppConfig.Service.port}")

    Future {
      val startTime = System.currentTimeMillis()
      val response = backend.send(request)
      val duration = System.currentTimeMillis() - startTime
      logger.debug(s"Cloud service response received in ${duration}ms")

      response.body match {
        case Right(jsonString) =>
          try {
            logger.debug("Parsing JSON response from cloud service")
            val json = Json.parse(jsonString)
            val response = (json \ "response").as[String]
            logger.debug(s"Successfully parsed response: ${response.take(100)}${if (response.length > 100) "..." else ""}")
            response
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

  def generateOllamaResponse(previousResponse: String): String = {
    logger.debug(s"Generating Ollama response for: ${previousResponse.take(100)}${if (previousResponse.length > 100) "..." else ""}")

    val prompt = s"how can you respond to the statement: $previousResponse"
    val startTime = System.currentTimeMillis()

    Try {
      logger.debug(s"Sending request to Ollama using model: ${AppConfig.Ollama.model}")
      val result = ollamaAPI.generate(
        AppConfig.Ollama.model,
        prompt,
        false,
        new ollamaUtils.Options(new java.util.HashMap[String, Object]())
      )
      val duration = System.currentTimeMillis() - startTime
      logger.debug(s"Ollama response received in ${duration}ms")
      result.getResponse
    } match {
      case Success(response) =>
        logger.debug(s"Successfully generated Ollama response: ${response.take(100)}${if (response.length > 100) "..." else ""}")
        response
      case Failure(e) =>
        logger.error(s"Ollama error: ${e.getMessage}", e)
        throw new Exception(s"Ollama error: ${e.getMessage}")
    }
  }

  def runConversation(initialQuery: String): Seq[ConversationTurn] = {
    logger.info("Starting new conversation")
    logger.debug(s"Initial query: ${initialQuery.take(100)}${if (initialQuery.length > 100) "..." else ""}")

    var conversation = Seq.empty[ConversationTurn]
    var currentQuery = initialQuery
    val startTime = LocalDateTime.now()

    logger.debug(s"Conversation parameters: maxTurns=${AppConfig.Conversation.maxTurns}, timeoutMinutes=${AppConfig.Conversation.timeoutMinutes}")

    while (
      conversation.length < AppConfig.Conversation.maxTurns &&
        startTime.plusMinutes(AppConfig.Conversation.timeoutMinutes).isAfter(LocalDateTime.now())
    ) {
      val turnStartTime = System.currentTimeMillis()
      val turnNumber = conversation.length + 1
      logger.info(s"Starting conversation turn $turnNumber")

      try {
        logger.debug(s"Processing query for turn $turnNumber: ${currentQuery.take(100)}${if (currentQuery.length > 100) "..." else ""}")

        // Get cloud response
        val cloudResponse = Await.result(
          generateCloudResponse(currentQuery),
          AppConfig.CloudService.requestTimeoutSeconds.seconds
        )
        logger.info(s"Turn $turnNumber - Cloud Response received: ${cloudResponse.take(100)}${if (cloudResponse.length > 100) "..." else ""}")

        // Generate next query using Ollama
        val ollamaResponse = generateOllamaResponse(cloudResponse)
        logger.info(s"Turn $turnNumber - Ollama Response received: ${ollamaResponse.take(100)}${if (ollamaResponse.length > 100) "..." else ""}")

        // Record the turn
        val processingTime = System.currentTimeMillis() - turnStartTime
        conversation = conversation :+ ConversationTurn(
          LocalDateTime.now(),
          currentQuery,
          cloudResponse,
          ollamaResponse,
          processingTime
        )
        logger.debug(s"Turn $turnNumber completed in ${processingTime}ms")

        // Set up next turn
        currentQuery = ollamaResponse
      } catch {
        case e: Exception =>
          logger.error(s"Error during conversation turn $turnNumber: ${e.getMessage}", e)
          throw e
      }
    }

    val totalTime = System.currentTimeMillis() - startTime.toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
    logger.info(s"Conversation completed with ${conversation.length} turns in ${totalTime}ms")
    conversation
  }

  def saveConversationToCSV(conversation: Seq[ConversationTurn], filepath: String): Unit = {
    logger.info(s"Saving conversation with ${conversation.length} turns to CSV: $filepath")

    try {
      val header = "timestamp,query,cloud_response,ollama_response,processing_time_ms\n"
      val rows = conversation.map { turn =>
        s"${turn.timestamp},${escape(turn.query)},${escape(turn.cloudResponse)},${escape(turn.ollamaResponse)},${turn.processingTimeMs}"
      }.mkString("\n")

      val path = Paths.get(filepath)
      logger.debug(s"Writing ${conversation.length} rows to ${path.toAbsolutePath}")

      Files.write(
        path,
        (header + rows).getBytes(StandardCharsets.UTF_8)
      )
      logger.info(s"Successfully saved conversation to: ${path.toAbsolutePath}")
    } catch {
      case e: Exception =>
        logger.error(s"Failed to save conversation to CSV: $filepath", e)
        throw e
    }
  }

  private def escape(s: String): String = {
    "\"" + s.replace("\"", "\"\"") + "\""
  }
}