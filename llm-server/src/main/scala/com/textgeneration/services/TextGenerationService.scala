package com.textgeneration.services

import com.textgeneration.models.{GenerationRequest, GenerationResponse, ResponseMetadata}
import com.textgeneration.{BedrockRequest, BedrockResponse}
import com.textgeneration.clients.ProtoHttpClient
import scala.concurrent.{Future, ExecutionContext}
import org.slf4j.LoggerFactory

class TextGenerationService(protoClient: ProtoHttpClient)(implicit ec: ExecutionContext) {
  private val logger = LoggerFactory.getLogger(getClass)

  def generateText(request: GenerationRequest): Future[GenerationResponse] = {
    logger.info(s"Processing request: $request")

    // Convert JSON request to Protobuf format expected by Lambda
    val protoRequest = BedrockRequest(
      inputText = request.query,
      parameters = Map(
        "max_tokens" -> request.max_length.toString,
        "temperature" -> request.temperature.toString
      )
    )

    // Send request and convert Protobuf response back to JSON
    protoClient.generateText(protoRequest).map { protoResponse =>
      GenerationResponse(
        response = protoResponse.outputText,
        metadata = ResponseMetadata(
          length = protoResponse.tokens.length,
          stop_reason = "stop_sequence", // Default from Bedrock
          processing_time_ms = 0, // Could be calculated if needed
          model = "anthropic.claude-v2"
        )
      )
    }
  }
}