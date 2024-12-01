package models

import java.time.LocalDateTime
import play.api.libs.json._

case class LLMRequest(
                       inputText: String,
                       temperature: Double,
                       maxTokens: Int
                     )

case class LLMResponse(
                        response: String
                      )

case class ConversationTurn(
                             timestamp: LocalDateTime,
                             query: String,
                             cloudResponse: String,
                             ollamaResponse: String,
                             processingTimeMs: Long
                           )

object Models {
  implicit val llmRequestFormat: Format[LLMRequest] = Json.format[LLMRequest]
  implicit val llmResponseFormat: Format[LLMResponse] = Json.format[LLMResponse]
  implicit val conversationTurnFormat: Format[ConversationTurn] = Json.format[ConversationTurn]
}