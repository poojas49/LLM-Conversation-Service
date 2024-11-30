package com.textgeneration.clients

import com.textgeneration.models._
import sttp.client3._
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization.write
import scala.concurrent.{Future, ExecutionContext}
import org.slf4j.LoggerFactory

class LambdaClient(apiGatewayUrl: String)(implicit ec: ExecutionContext) {
  private val logger = LoggerFactory.getLogger(getClass)
  private val backend = HttpURLConnectionBackend()

  implicit val formats: Formats = DefaultFormats

  def generateText(request: GenerationRequest): Future[GenerationResponse] = {
    logger.info(s"Sending request to Lambda: $request")

    val requestBody = Map(
      "query" -> request.query,
      "max_length" -> request.maxLength.getOrElse(100),
      "temperature" -> request.temperature.getOrElse(0.7)
    )

    val lambdaRequest = basicRequest
      .post(uri"$apiGatewayUrl")
      .header("Content-Type", "application/json")
      .body(write(requestBody))

    Future {
      val response = lambdaRequest.send(backend)
      response.body match {
        case Right(jsonStr) =>
          logger.debug(s"Received response from Lambda: $jsonStr")
          try {
            val parsed = parse(jsonStr)

            // Direct parsing of response and metadata
            GenerationResponse(
              response = (parsed \ "response").extract[String],
              metadata = Some(ResponseMetadata(
                length = (parsed \ "metadata" \ "length").extract[Int],
                stopReason = (parsed \ "metadata" \ "stop_reason").extract[String]
              ))
            )
          } catch {
            case e: Exception =>
              logger.error(s"Failed to parse Lambda response: $jsonStr", e)
              throw new RuntimeException(s"Failed to parse Lambda response: ${e.getMessage}")
          }

        case Left(error) =>
          logger.error(s"Lambda request failed: $error")
          throw new RuntimeException(s"API Gateway request failed: $error")
      }
    }
  }
}