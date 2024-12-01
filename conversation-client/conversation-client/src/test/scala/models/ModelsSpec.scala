package models

import models.ApiModels.conversationTurnFormat
import models.Models.llmRequestFormat
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json._

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ModelsSpec extends AnyFlatSpec with Matchers {
  "LLMRequest" should "serialize to JSON correctly" in {
    val request = LLMRequest("test query", 0.7, 100)
    val json = Json.toJson(request)

    (json \ "inputText").as[String] shouldBe "test query"
    (json \ "temperature").as[Double] shouldBe 0.7
    (json \ "maxTokens").as[Int] shouldBe 100
  }

  it should "deserialize from JSON correctly" in {
    val jsonStr = """{"inputText": "test", "temperature": 0.7, "maxTokens": 100}"""
    val request = Json.parse(jsonStr).as[LLMRequest]

    request.inputText shouldBe "test"
    request.temperature shouldBe 0.7
    request.maxTokens shouldBe 100
  }

  "ConversationTurn" should "handle serialization with timestamps" in {
    val now = LocalDateTime.now()
    val turn = ConversationTurn(
      timestamp = now,
      query = "test query",
      cloudResponse = "cloud response",
      ollamaResponse = "ollama response",
      processingTimeMs = 1000
    )

    val json = Json.toJson(turn)
    val parsed = Json.parse(Json.toJson(turn).toString()).as[ConversationTurn]

    parsed.timestamp shouldBe turn.timestamp
    parsed.query shouldBe turn.query
    parsed.cloudResponse shouldBe turn.cloudResponse
    parsed.ollamaResponse shouldBe turn.ollamaResponse
    parsed.processingTimeMs shouldBe turn.processingTimeMs
  }

  "ConversationRequest" should "handle optional outputFile" in {
    val request1 = ConversationRequest("query", None)
    val request2 = ConversationRequest("query", Some("output.csv"))

    request1.outputFile shouldBe None
    request2.outputFile shouldBe Some("output.csv")
  }

  "ConversationResponse" should "calculate metrics correctly" in {
    val turns = Seq(
      ConversationTurn(LocalDateTime.now(), "q1", "c1", "o1", 100),
      ConversationTurn(LocalDateTime.now(), "q2", "c2", "o2", 200)
    )

    val response = ConversationResponse(
      turns = turns.length,
      averageProcessingTimeMs = turns.map(_.processingTimeMs).sum / turns.length,
      conversation = turns
    )

    response.turns shouldBe 2
    response.averageProcessingTimeMs shouldBe 150
    response.conversation should have length 2
  }
}