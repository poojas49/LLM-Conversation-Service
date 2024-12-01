package com.example.services

import com.example.proto.messages.BedrockRequest
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ProtobufServiceTest extends AnyFlatSpec with Matchers {

  "ProtobufService" should "handle JSON conversion" in {
    val service = new ProtobufService()

    val request = BedrockRequest(
      inputText = "Hello",
      parameters = Map("temperature" -> "0.7")
    )

    val jsonResult = service.protoToJson(request)
    jsonResult.isSuccess shouldBe true

    val json = jsonResult.get
    val protoResult = service.jsonToProto[BedrockRequest](json)
    protoResult.isSuccess shouldBe true

    val converted = protoResult.get
    converted.inputText shouldBe "Hello"
    converted.parameters shouldBe Map("temperature" -> "0.7")
  }
}