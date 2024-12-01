package api

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import akka.http.scaladsl.model.{HttpEntity, ContentTypes}
import models.ConversationRequest
import play.api.libs.json._
import services.ConversationalAgent

class ConversationRoutesSpec extends AnyFlatSpec with Matchers {
  val conversationalAgent = new ConversationalAgent()
  val routes = new ConversationRoutes(conversationalAgent)

  it should "handle invalid JSON request" in {
    val invalidJson = """{"invalid": "json""""
    val requestEntity = HttpEntity(
      ContentTypes.`application/json`,
      invalidJson
    )
  }
}