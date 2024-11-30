package com.textgeneration.models

// Request/Response Models
case class GenerationRequest(query: String, maxLength: Option[Int] = None, temperature: Option[Double] = None)
case class GenerationResponse(response: String, metadata: Option[ResponseMetadata] = None)
case class ResponseMetadata(length: Int, stopReason: String)

// Cache Model
case class CacheEntry(response: GenerationResponse, timestamp: Long)

// JSON Support trait
import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val responseMetadataFormat: RootJsonFormat[ResponseMetadata] = jsonFormat2(ResponseMetadata)
  implicit val generationResponseFormat: RootJsonFormat[GenerationResponse] = jsonFormat2(GenerationResponse)
  implicit val generationRequestFormat: RootJsonFormat[GenerationRequest] = jsonFormat3(GenerationRequest)
}