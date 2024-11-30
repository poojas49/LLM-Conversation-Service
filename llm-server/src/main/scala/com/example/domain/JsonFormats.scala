package com.example.domain

import spray.json.DefaultJsonProtocol

object JsonFormats extends DefaultJsonProtocol {
  implicit val requestContextFormat = jsonFormat1(RequestContext)
  implicit val llmRequestFormat = jsonFormat3(LlmRequest)
  implicit val apiGatewayRequestFormat = jsonFormat2(ApiGatewayRequest)
  implicit val apiGatewayResponseFormat = jsonFormat3(ApiGatewayResponse)
  implicit val llmResponseFormat = jsonFormat1(LlmResponse)
  implicit val errorResponseFormat = jsonFormat1(ErrorResponse)
}