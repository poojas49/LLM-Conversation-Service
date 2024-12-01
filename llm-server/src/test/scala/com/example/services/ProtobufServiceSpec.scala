package com.example.services

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.example.proto.messages.BedrockRequest

class ProtobufServiceSpec extends AnyFlatSpec with Matchers {
  val service = new ProtobufService()

  "ProtobufService" should "encode and decode messages" in {
    val originalRequest = BedrockRequest(
      inputText = "Test message",
      parameters = Map("temp" -> "0.7")
    )

    val encoded = service.encodeToBase64(originalRequest)
    encoded.isSuccess shouldBe true

    val decoded = service.decodeFromBase64[BedrockRequest](encoded.get)
    decoded.isSuccess shouldBe true
    decoded.get.inputText shouldBe "Test message"
    decoded.get.parameters shouldBe Map("temp" -> "0.7")
  }

  it should "handle empty messages" in {
    val emptyRequest = BedrockRequest()
    val result = service.encodeToBase64(emptyRequest)
    result.isSuccess shouldBe true
  }

  it should "fail on invalid base64" in {
    val result = service.decodeFromBase64[BedrockRequest]("invalid-base64")
    result.isFailure shouldBe true
  }
}