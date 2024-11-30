package com.example

import com.example.proto.messages.{BedrockRequest, BedrockResponse}
import java.util.Base64
import scalapb.json4s.JsonFormat

object TestHelper {
  def createTestBase64Request(): String = {
    try {
      val request = BedrockRequest(
        inputText = "What is the capital of France?",
        parameters = Map(
          "temperature" -> "0.7",
          "max_tokens" -> "150"  // This will be mapped to max_tokens_to_sample in the service
        )
      )

      val base64String = Base64.getEncoder.encodeToString(request.toByteArray)
      println("Successfully created base64 string:")
      println(base64String)

      println("\nRequest as JSON:")
      println(JsonFormat.toJsonString(request))

      base64String
    } catch {
      case e: Exception =>
        println(s"Error creating test request: ${e.getMessage}")
        e.printStackTrace()
        throw e
    }
  }

  def main(args: Array[String]): Unit = {
    println("Generating test request...")
    createTestBase64Request()
  }
}