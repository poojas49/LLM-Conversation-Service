package com.textgeneration.clients

import com.textgeneration.proto.messages.{BedrockRequest, BedrockResponse}
import scala.concurrent.{Future, ExecutionContext}
import com.typesafe.scalalogging.LazyLogging
import java.util.Base64
import sttp.client3._
import sttp.client3.asynchttpclient.future.AsyncHttpClientFutureBackend

class ProtoHttpClient(baseUrl: String)(implicit ec: ExecutionContext) extends LazyLogging {
  private val backend = AsyncHttpClientFutureBackend()

  def generateText(request: BedrockRequest): Future[BedrockResponse] = {
    // Convert protobuf to base64
    val base64Request = Base64.getEncoder.encodeToString(request.toByteArray)

    logger.info("Sending request to Lambda function")

    val request = basicRequest
      .get(uri"$baseUrl/generate?query=$base64Request")
      .response(asByteArray)

    backend.send(request).map { response =>
      response.body match {
        case Right(bytes) =>
          // Decode base64 response back to protobuf
          val responseBytes = Base64.getDecoder.decode(new String(bytes))
          BedrockResponse.parseFrom(responseBytes)

        case Left(error) =>
          logger.error(s"Error from Lambda: $error")
          throw new RuntimeException(s"Failed to generate text: $error")
      }
    }
  }
}