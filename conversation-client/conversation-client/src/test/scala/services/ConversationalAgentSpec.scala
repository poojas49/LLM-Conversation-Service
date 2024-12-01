package services

import config.AppConfig
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import models.ConversationTurn

import scala.concurrent.ExecutionContext.Implicits.global
import java.nio.file.{Files, Paths}
import java.time.LocalDateTime

class ConversationalAgentSpec extends AnyFlatSpec with Matchers with ScalaFutures {
  implicit override val patienceConfig = PatienceConfig(timeout = Span(10, Seconds))

  val agent = new ConversationalAgent()

  it should "generate Ollama response" in {
    val previousResponse = "test response"
    val response = agent.generateOllamaResponse(previousResponse)
    response should not be empty
  }

  it should "save conversation to CSV" in {
    val testFile = "test_conversation.csv"
    val conversation = Seq(
      ConversationTurn(
        LocalDateTime.now(),
        "test query",
        "cloud response",
        "ollama response",
        1000L
      )
    )

    agent.saveConversationToCSV(conversation, testFile)

    val path = Paths.get(testFile)
    Files.exists(path) shouldBe true

    val content = new String(Files.readAllBytes(path))
    content should include("test query")
    content should include("cloud response")
    content should include("ollama response")

    // Cleanup
    Files.delete(path)
  }
}