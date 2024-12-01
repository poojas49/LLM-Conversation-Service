package com.example.services

import scalapb.GeneratedMessage
import java.util.Base64
import scala.util.Try
import scalapb.json4s.JsonFormat

class ProtobufService {
  def decodeFromBase64[T <: GeneratedMessage](base64String: String)(implicit companion: scalapb.GeneratedMessageCompanion[T]): Try[T] = Try {
    val bytes = Base64.getDecoder.decode(base64String)
    companion.parseFrom(bytes)
  }

  def encodeToBase64(message: GeneratedMessage): Try[String] = Try {
    Base64.getEncoder.encodeToString(message.toByteArray)
  }

  def jsonToProto[T <: GeneratedMessage](json: String)(implicit companion: scalapb.GeneratedMessageCompanion[T]): Try[T] = Try {
    // Using ScalaPB's JsonFormat
    JsonFormat.fromJsonString[T](json)(companion)
  }

  def protoToJson(message: GeneratedMessage): Try[String] = Try {
    // Using ScalaPB's JsonFormat
    JsonFormat.toJsonString(message)
  }
}