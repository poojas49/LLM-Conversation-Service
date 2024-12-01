package models

import play.api.libs.json._
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

case class ConversationRequest(
                                initialQuery: String,
                                outputFile: Option[String] = None
                              )

case class ConversationResponse(
                                 turns: Int,
                                 averageProcessingTimeMs: Long,
                                 conversation: Seq[ConversationTurn]
                               )

object ApiModels {
  // DateTime formatter
  private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

  // LocalDateTime format
  implicit val localDateTimeFormat: Format[LocalDateTime] = new Format[LocalDateTime] {
    def reads(json: JsValue): JsResult[LocalDateTime] = json.validate[String].map(LocalDateTime.parse(_, dateFormatter))
    def writes(date: LocalDateTime): JsValue = JsString(date.format(dateFormatter))
  }

  // ConversationTurn format
  implicit val conversationTurnFormat: Format[ConversationTurn] = Json.format[ConversationTurn]

  // Request/Response formats
  implicit val conversationRequestFormat: Format[ConversationRequest] = Json.format[ConversationRequest]
  implicit val conversationResponseFormat: Format[ConversationResponse] = Json.format[ConversationResponse]
}