/**
 * Data Models
 *
 * Design Principles:
 * - Immutable case classes for thread safety
 * - Clear separation between API and internal models
 * - JSON serialization support via Play JSON
 * - Consistent timestamp handling
 */
package models

import java.time.LocalDateTime
import play.api.libs.json._

/**
 * LLM-specific models for interaction with AI services
 * Separates concerns between request and response handling
 */
case class LLMRequest(
                       inputText: String,
                       temperature: Double,
                       maxTokens: Int
                     )

case class LLMResponse(
                        response: String
                      )

/**
 * Represents a single conversation exchange
 * Records timing and responses from both services
 */
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