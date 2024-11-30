package com.example

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.amazonaws.services.lambda.runtime.events.{APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent}
import com.example.proto.messages.{BedrockRequest, BedrockResponse}
import com.example.services.{BedrockService, ProtobufService}
import scala.jdk.CollectionConverters._
import com.example.config.AppConfig
import scala.util.{Success, Failure, Try}
import java.util.Base64

class LambdaHandler extends RequestHandler[APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent] {

  private val protobufService = new ProtobufService()
  private val bedrockService = new BedrockService(AppConfig.bedrockConfig)

  override def handleRequest(
                              input: APIGatewayProxyRequestEvent,
                              context: Context
                            ): APIGatewayProxyResponseEvent = {

    val logger = context.getLogger

    try {
      val base64Input = Option(input)
        .flatMap(req => Option(req.getQueryStringParameters))
        .flatMap(params => Option(params.get("query")))
        .getOrElse(throw new IllegalArgumentException("Missing required 'query' parameter"))

      (for {
        // Decode and parse the protobuf request
        protoRequest <- protobufService.decodeFromBase64[BedrockRequest](base64Input)

        // Get Bedrock response as protobuf
        protoResponse <- bedrockService.invokeModel(protoRequest)

        // Encode response to base64
        base64Response <- Try(Base64.getEncoder.encodeToString(protoResponse.toByteArray))
      } yield base64Response) match {
        case Success(base64Response) =>
          new APIGatewayProxyResponseEvent()
            .withStatusCode(200)
            .withBody(base64Response)
            .withHeaders(Map(
              "Content-Type" -> "application/x-protobuf",
              "X-Request-ID" -> Option(input.getRequestContext)
                .map(_.getRequestId)
                .getOrElse("unknown")
            ).asJava)

        case Failure(e) =>
          logger.log(s"Error processing request: ${e.getMessage}")
          new APIGatewayProxyResponseEvent()
            .withStatusCode(400)
            .withBody(s"Error: ${e.getMessage}")
      }

    } catch {
      case e: IllegalArgumentException =>
        logger.log(s"Bad Request: ${e.getMessage}")
        new APIGatewayProxyResponseEvent()
          .withStatusCode(400)
          .withBody(s"Bad Request: ${e.getMessage}")

      case e: Exception =>
        logger.log(s"Error processing request: ${e.getMessage}")
        e.printStackTrace()
        new APIGatewayProxyResponseEvent()
          .withStatusCode(500)
          .withBody(s"Internal Server Error: ${e.getMessage}")
    }
  }
}