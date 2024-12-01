package com.example

import com.example.proto.messages.{BedrockRequest, BedrockResponse}

import java.util.Base64

object TestClient {
  def main(args: Array[String]): Unit = {
    // Create test request
    val request = BedrockRequest(
      inputText = "What is the capital of France?",
      parameters = Map(
        "temperature" -> "0.7",
        "max_tokens" -> "150"
      )
    )

    // Encode request
    val base64Request = Base64.getEncoder.encodeToString(request.toByteArray)
    println(s"Base64 Request: $base64Request")

    println("\nCopy this request and use it in your Lambda test event. When you get the response, paste it below.")

    // Read response from stdin
    println("\nPaste the base64 response here:")
    val base64Response = scala.io.StdIn.readLine()

    try {
      // Decode and print response
      val bytes = Base64.getDecoder.decode(base64Response)
      val response = BedrockResponse.parseFrom(bytes)
      println(s"\nDecoded Response:")
      println(s"Output Text: ${response.outputText}")
      if (response.confidenceScores.nonEmpty) {
        println(s"Confidence Scores: ${response.confidenceScores}")
      }
      if (response.tokens.nonEmpty) {
        println(s"Tokens: ${response.tokens.mkString(", ")}")
      }
    } catch {
      case e: Exception =>
        println(s"Error decoding response: ${e.getMessage}")
        e.printStackTrace()
    }
  }
}