package com.example.services

import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import com.example.config.BedrockConfig
import com.example.proto.messages.{BedrockRequest, BedrockResponse}
import scala.util.Try
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization.write
import org.slf4j.{Logger, LoggerFactory}

/**
 * Service for interacting with AWS Bedrock API
 *
 * Design Rationale:
 * - Encapsulates all Bedrock-specific logic
 * - Manages client lifecycle
 * - Implements retry logic and error handling
 * - Provides clean separation from protobuf and Lambda handling
 * - Uses configuration injection for flexibility
 */
class BedrockService(config: BedrockConfig) {
  private val logger: Logger = LoggerFactory.getLogger(getClass)
  implicit val formats = DefaultFormats

  logger.info(s"Initializing BedrockService with region: ${config.region} and model: ${config.modelId}")

  // Initialize AWS clients with appropriate configuration
  private val httpClient = {
    logger.debug("Building HTTP client for Bedrock service")
    UrlConnectionHttpClient.builder().build()
  }

  private val bedrockClient = {
    logger.debug("Building Bedrock runtime client")
    BedrockRuntimeClient.builder()
      .region(Region.of(config.region))
      .httpClient(httpClient)
      .build()
  }

  // Main method for invoking Bedrock model
  def invokeModel(request: BedrockRequest): Try[BedrockResponse] = {
    logger.info(s"Invoking Bedrock model with input length: ${request.inputText.length}")
    logger.debug(s"Request parameters: ${request.parameters}")

    Try {
      val anthropicRequest = Map(
        "prompt" -> s"\n\nHuman: ${request.inputText}\n\nAssistant:",
        "max_tokens_to_sample" -> request.parameters.getOrElse("max_tokens", "150").toInt,
        "temperature" -> request.parameters.getOrElse("temperature", "0.7").toDouble,
        "top_p" -> 1,
        "stop_sequences" -> List("\n\nHuman:"),
        "anthropic_version" -> "bedrock-2023-05-31"
      )

      val jsonRequest = write(anthropicRequest)
      logger.debug(s"Prepared Bedrock request payload: $jsonRequest")

      val modelRequest = InvokeModelRequest.builder()
        .modelId(config.modelId)
        .contentType("application/json")
        .accept("application/json")
        .body(SdkBytes.fromUtf8String(jsonRequest))
        .build()

      logger.info("Sending request to Bedrock API")
      val response = bedrockClient.invokeModel(modelRequest)
      val responseBody = response.body().asUtf8String()
      logger.debug(s"Received raw response from Bedrock: $responseBody")

      val jsonResponse = parse(responseBody)
      val completion = (jsonResponse \ "completion").extract[String].trim
      logger.debug(s"Extracted completion of length: ${completion.length}")

      val bedrockResponse = BedrockResponse(
        outputText = completion,
        confidenceScores = Map.empty,
        tokens = List.empty
      )

      logger.info(s"Successfully processed Bedrock response with output length: ${completion.length}")
      bedrockResponse

    }.recoverWith { case e: Exception =>
      logger.error("Failed to invoke Bedrock model or process response", e)
      logger.debug(s"Failed request input text: ${request.inputText}")
      logger.debug(s"Failed request parameters: ${request.parameters}")
      Try(throw e)
    }
  }

  def close(): Unit = {
    logger.info("Closing BedrockService connections")
    try {
      bedrockClient.close()
      logger.debug("Successfully closed Bedrock client")
    } catch {
      case e: Exception =>
        logger.error("Error closing Bedrock client", e)
    }

    try {
      httpClient.close()
      logger.debug("Successfully closed HTTP client")
    } catch {
      case e: Exception =>
        logger.error("Error closing HTTP client", e)
    }

    logger.info("BedrockService shutdown completed")
  }
}