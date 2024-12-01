package com.example.domain

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DomainSpec extends AnyFlatSpec with Matchers {

  "LlmRequest" should "handle optional parameters" in {
    val request1 = LlmRequest("test")
    request1.temperature shouldBe None
    request1.maxTokens shouldBe None

    val request2 = LlmRequest("test", Some(0.7), Some(100))
    request2.temperature shouldBe Some(0.7)
    request2.maxTokens shouldBe Some(100)
  }

  "RequestContext" should "store request ID" in {
    val context = RequestContext("test-id")
    context.requestId shouldBe "test-id"
  }

  "ApiGatewayRequest" should "store parameters and context" in {
    val context = RequestContext("test-id")
    val request = ApiGatewayRequest(
      queryStringParameters = Map("param" -> "value"),
      requestContext = context
    )

    request.queryStringParameters should contain ("param" -> "value")
    request.requestContext.requestId shouldBe "test-id"
  }

  "ApiGatewayResponse" should "store status, headers and body" in {
    val response = ApiGatewayResponse(
      statusCode = 200,
      headers = Map("Content-Type" -> "application/json"),
      body = "test body"
    )

    response.statusCode shouldBe 200
    response.headers should contain ("Content-Type" -> "application/json")
    response.body shouldBe "test body"
  }

  "LlmResponse" should "store response text" in {
    val response = LlmResponse("test response")
    response.response shouldBe "test response"
  }

  "ErrorResponse" should "store error message" in {
    val error = ErrorResponse("test error")
    error.error shouldBe "test error"
  }
}