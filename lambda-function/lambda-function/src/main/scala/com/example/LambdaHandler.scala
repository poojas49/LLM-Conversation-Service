package com.example

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.amazonaws.services.lambda.runtime.events.{APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent}
import com.example.proto.messages.{BedrockRequest, BedrockResponse}
import com.example.services.{BedrockService, ProtobufService}
import org.slf4j.{Logger, LoggerFactory}
import scala.jdk.CollectionConverters._
import com.example.config.AppConfig
import scala.util.{Success, Failure, Try}
import java.util.Base64

/**
 * Main AWS Lambda handler for processing API Gateway requests
 *
 * Design Rationale:
 * - Implements RequestHandler for seamless AWS Lambda integration
 * - Uses dependency injection for services to facilitate testing
 * - Implements comprehensive error handling and logging
 * - Processes protobuf messages for efficient data transfer
 * - Maintains separation of concerns between Lambda handling, Bedrock interaction, and protobuf processing
 */
class LambdaHandler extends RequestHandler[APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent] {

  private val logger: Logger = LoggerFactory.getLogger(getClass)
  private val protobufService = new ProtobufService()
  private val bedrockService = new BedrockService(AppConfig.bedrockConfig)

  override def handleRequest(
                              input: APIGatewayProxyRequestEvent,
                              context: Context
                            ): APIGatewayProxyResponseEvent = {
    // Extract request ID for tracing
    val requestId = Option(input.getRequestContext)
      .map(_.getRequestId)
      .getOrElse("unknown")

    logger.info(s"Processing request with ID: $requestId")
    logger.debug(s"Input event: $input")

    // Main processing flow using for-comprehension for clean error handling
    try {
      // Extract and validate required query parameter
      val base64Input = Option(input)
        .flatMap(req => Option(req.getQueryStringParameters))
        .flatMap(params => Option(params.get("query")))
        .getOrElse {
          logger.error(s"Request $requestId missing required 'query' parameter")
          throw new IllegalArgumentException("Missing required 'query' parameter")
        }

      logger.debug(s"Request $requestId received base64 input of length: ${base64Input.length}")

      // Process request using for-comprehension for sequential operations
      (for {
        // Decode and parse the protobuf request
        protoRequest <- {
          logger.debug(s"Request $requestId attempting to decode protobuf request")
          protobufService.decodeFromBase64[BedrockRequest](base64Input)
        }

        // Get Bedrock response as protobuf
        protoResponse <- {
          logger.info(s"Request $requestId invoking Bedrock model")
          bedrockService.invokeModel(protoRequest)
        }

        // Encode response to base64
        base64Response <- {
          logger.debug(s"Request $requestId encoding protobuf response to base64")
          Try(Base64.getEncoder.encodeToString(protoResponse.toByteArray))
        }
      } yield base64Response) match {
        case Success(base64Response) =>
          logger.info(s"Request $requestId completed successfully")
          logger.debug(s"Response length: ${base64Response.length}")

          new APIGatewayProxyResponseEvent()
            .withStatusCode(200)
            .withBody(base64Response)
            .withHeaders(Map(
              "Content-Type" -> "application/x-protobuf",
              "X-Request-ID" -> requestId
            ).asJava)

        case Failure(e) =>
          logger.error(s"Request $requestId failed during processing", e)
          // Handle processing failures
          new APIGatewayProxyResponseEvent()
            .withStatusCode(400)
            .withBody(s"Error: ${e.getMessage}")
      }

    } catch {
      // Specific error handling for different failure types
      case e: IllegalArgumentException =>
        logger.error(s"Request $requestId failed with bad request", e)
        new APIGatewayProxyResponseEvent()
          .withStatusCode(400)
          .withBody(s"Bad Request: ${e.getMessage}")

      case e: Exception =>
        logger.error(s"Request $requestId failed with internal server error", e)
        new APIGatewayProxyResponseEvent()
          .withStatusCode(500)
          .withBody(s"Internal Server Error: ${e.getMessage}")
    }
  }
}