package com.example.services

import com.example.config.ServiceConfig
import com.example.domain._
import com.example.domain.JsonFormats._
import com.example.proto.messages.{BedrockRequest, BedrockResponse}
import sttp.client3._
import sttp.client3.sprayJson._
import spray.json._
import scala.concurrent.{ExecutionContext, Future}
import java.util.UUID
import org.slf4j.LoggerFactory

/**
 * LLM Service Implementation
 * =========================
 *
 * Core service for handling Language Model requests and responses. Manages the complete
 * lifecycle of LLM query processing.
 *
 * Design Rationale:
 * ----------------
 * - Non-blocking operations using Future for scalability
 * - Protocol Buffers for efficient serialization
 * - Comprehensive error handling and recovery
 * - Detailed request tracing through logging
 *
 * Key Features:
 * -----------
 * 1. Request Validation & Normalization
 * 2. Protocol Buffer Message Creation
 * 3. Base64 Encoding/Decoding
 * 4. API Gateway Integration
 * 5. Response Processing
 *
 * Processing Pipeline:
 * -----------------
 * 1. Validate and normalize input parameters
 * 2. Create Protobuf request
 * 3. Encode for transport
 * 4. Send to API Gateway
 * 5. Process and decode response
 * 6. Handle errors at each stage
 */
class LlmService(config: ServiceConfig, protobufService: ProtobufService)(implicit ec: ExecutionContext) {
  private val logger = LoggerFactory.getLogger(getClass)
  private val backend = HttpURLConnectionBackend()

  def processQuery(request: LlmRequest): Future[String] = {
    logger.info(s"Starting LLM query processing")
    logger.debug(s"Processing request with input text length: ${request.inputText.length}")
    logger.trace(s"Request details - Temperature: ${request.temperature}, MaxTokens: ${request.maxTokens}")

    val protoRequest = {
      logger.debug("Creating BedrockRequest with configured parameters")
      val temp = request.temperature.getOrElse(config.llm.defaultTemperature)
      val tokens = request.maxTokens.getOrElse(config.llm.defaultMaxTokens)

      logger.trace(s"Using temperature: $temp, max tokens: $tokens")
      BedrockRequest(
        inputText = request.inputText,
        parameters = Map(
          "temperature" -> temp.toString,
          "max_tokens" -> tokens.toString
        )
      )
    }
    logger.debug("BedrockRequest created successfully")

    for {
      base64Request <- {
        logger.debug("Encoding proto request to base64")
        Future.fromTry(protobufService.encodeToBase64(protoRequest))
          .andThen {
            case scala.util.Success(_) => logger.debug("Successfully encoded request to base64")
            case scala.util.Failure(ex) =>
              logger.error("Failed to encode proto request to base64", ex)
              logger.warn("Query processing will fail due to encoding error")
          }
      }

      apiRequest = {
        val requestId = UUID.randomUUID().toString
        logger.debug(s"Creating API Gateway request with ID: $requestId")
        logger.trace(s"Base64 request length: ${base64Request.length}")

        ApiGatewayRequest(
          queryStringParameters = Map("query" -> base64Request),
          requestContext = RequestContext(requestId)
        )
      }

      response <- {
        logger.info(s"Sending request to API Gateway: ${config.apiGatewayUrl}")
        Future {
          basicRequest
            .post(uri"${config.apiGatewayUrl}")
            .header("Content-Type", config.llm.headers("Content-Type"))
            .body(apiRequest.toJson.compactPrint)
            .response(asJson[ApiGatewayResponse])
            .send(backend)
            .body
            .fold(
              error => {
                logger.error(s"API Gateway request failed", new RuntimeException(error))
                logger.warn(s"Request ID ${apiRequest.requestContext.requestId} failed")
                logger.debug(s"Error details: $error")
                throw new RuntimeException(error)
              },
              response => {
                logger.info(s"Received response from API Gateway with status: ${response.statusCode}")
                logger.debug(s"Response body length: ${response.body.length}")
                logger.trace(s"Response headers: ${response.headers}")
                response
              }
            )
        }
      }

      decodedProto <- {
        logger.debug("Decoding API Gateway response from base64")
        Future.fromTry(
          protobufService.decodeFromBase64[BedrockResponse](response.body)
        ).andThen {
          case scala.util.Success(_) => logger.debug("Successfully decoded response")
          case scala.util.Failure(ex) =>
            logger.error("Failed to decode API Gateway response", ex)
            logger.warn(s"Request ID ${apiRequest.requestContext.requestId} failed at decoding stage")
        }
      }
    } yield {
      val outputLength = decodedProto.outputText.length
      logger.info(s"Successfully completed LLM query processing")
      logger.debug(s"Generated response length: $outputLength")
      logger.trace(s"Response preview: ${decodedProto.outputText.take(50)}...")
      decodedProto.outputText
    }
  }
}