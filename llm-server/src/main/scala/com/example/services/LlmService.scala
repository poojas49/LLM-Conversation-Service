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

class LlmService(config: ServiceConfig, protobufService: ProtobufService)(implicit ec: ExecutionContext) {
  private val backend = HttpURLConnectionBackend()

  def processQuery(request: LlmRequest): Future[String] = {
    val protoRequest = BedrockRequest(
      inputText = request.inputText,
      parameters = Map(
        "temperature" -> request.temperature.getOrElse(0.7).toString,
        "max_tokens" -> request.maxTokens.getOrElse(150).toString
      )
    )

    for {
      base64Request <- Future.fromTry(protobufService.encodeToBase64(protoRequest))

      apiRequest = ApiGatewayRequest(
        queryStringParameters = Map("query" -> base64Request),
        requestContext = RequestContext(UUID.randomUUID().toString)
      )

      response <- Future {
        basicRequest
          .post(uri"${config.apiGatewayUrl}")
          .header("Content-Type", "application/x-protobuf")
          .body(apiRequest.toJson.compactPrint)
          .response(asJson[ApiGatewayResponse])
          .send(backend)
          .body
          .fold(error => throw new RuntimeException(error), identity)
      }

      decodedProto <- Future.fromTry(
        protobufService.decodeFromBase64[BedrockResponse](response.body)
      )
    } yield decodedProto.outputText
  }
}