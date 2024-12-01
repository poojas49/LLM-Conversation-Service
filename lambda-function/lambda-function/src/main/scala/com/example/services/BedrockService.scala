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
import org.json4s.native.Serialization
import org.json4s.native.Serialization.write

class BedrockService(config: BedrockConfig) {
  implicit val formats = DefaultFormats

  private val httpClient = software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient.builder().build()

  private val bedrockClient = BedrockRuntimeClient.builder()
    .region(Region.of(config.region))
    .httpClient(httpClient)
    .build()

  def invokeModel(request: BedrockRequest): Try[BedrockResponse] = Try {
    val anthropicRequest = Map(
      "prompt" -> s"\n\nHuman: ${request.inputText}\n\nAssistant:",
      "max_tokens_to_sample" -> request.parameters.getOrElse("max_tokens", "150").toInt,
      "temperature" -> request.parameters.getOrElse("temperature", "0.7").toDouble,
      "top_p" -> 1,
      "stop_sequences" -> List("\n\nHuman:"),
      "anthropic_version" -> "bedrock-2023-05-31"
    )

    val jsonRequest = write(anthropicRequest)
    println(s"Sending request to Bedrock: $jsonRequest")

    val modelRequest = InvokeModelRequest.builder()
      .modelId(config.modelId)
      .contentType("application/json")
      .accept("application/json")
      .body(SdkBytes.fromUtf8String(jsonRequest))
      .build()

    val response = bedrockClient.invokeModel(modelRequest)
    val responseBody = response.body().asUtf8String()
    println(s"Received response from Bedrock: $responseBody")

    val jsonResponse = parse(responseBody)
    val completion = (jsonResponse \ "completion").extract[String].trim

    BedrockResponse(
      outputText = completion,
      confidenceScores = Map.empty,
      tokens = List.empty
    )
  }

  def close(): Unit = {
    bedrockClient.close()
    httpClient.close()
  }
}