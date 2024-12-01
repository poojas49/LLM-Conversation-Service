package com.example.domain

import spray.json.DefaultJsonProtocol


/**
 * JSON Serialization Formats
 * =========================
 *
 * Defines JSON serialization/deserialization formats for all domain models
 * using spray-json.
 *
 * Design Rationale:
 * ----------------
 * - Type-safe JSON conversion
 * - Automatic format derivation where possible
 * - Consistent serialization rules
 * - Integration with Akka HTTP
 *
 * Format Definitions:
 * -----------------
 * - requestContextFormat: Request tracking info
 * - llmRequestFormat: LLM generation parameters
 * - apiGatewayRequestFormat: API Gateway integration
 * - apiGatewayResponseFormat: Response wrapper
 * - llmResponseFormat: Generated text
 * - errorResponseFormat: Error messages
 */
object JsonFormats extends DefaultJsonProtocol {
  implicit val requestContextFormat = jsonFormat1(RequestContext)
  implicit val llmRequestFormat = jsonFormat3(LlmRequest)
  implicit val apiGatewayRequestFormat = jsonFormat2(ApiGatewayRequest)
  implicit val apiGatewayResponseFormat = jsonFormat3(ApiGatewayResponse)
  implicit val llmResponseFormat = jsonFormat1(LlmResponse)
  implicit val errorResponseFormat = jsonFormat1(ErrorResponse)
}