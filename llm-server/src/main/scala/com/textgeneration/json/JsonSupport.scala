package com.textgeneration.json

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.textgeneration.models._
import spray.json.DefaultJsonProtocol

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val generationRequestFormat = jsonFormat3(GenerationRequest)
  implicit val responseMetadataFormat = jsonFormat4(ResponseMetadata)
  implicit val generationResponseFormat = jsonFormat2(GenerationResponse)
}