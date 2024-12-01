package com.example.services

import scalapb.GeneratedMessage
import java.util.Base64
import scala.util.Try
import scalapb.json4s.JsonFormat
import org.slf4j.{Logger, LoggerFactory}

class ProtobufService {
  private val logger: Logger = LoggerFactory.getLogger(getClass)

  def decodeFromBase64[T <: GeneratedMessage](base64String: String)(implicit companion: scalapb.GeneratedMessageCompanion[T]): Try[T] = {
    logger.debug(s"Attempting to decode base64 string of length: ${base64String.length}")
    logger.trace(s"Decoding base64 string: $base64String")

    Try {
      val bytes = Base64.getDecoder.decode(base64String)
      logger.debug(s"Successfully decoded base64 to byte array of length: ${bytes.length}")

      val message = companion.parseFrom(bytes)
      logger.info(s"Successfully parsed protobuf message of type: ${companion.scalaDescriptor.fullName}")
      logger.debug(s"Parsed message: $message")
      message
    }.recoverWith { case e: Exception =>
      logger.error(s"Failed to decode base64 string to protobuf message of type ${companion.scalaDescriptor.fullName}", e)
      Try(throw e)
    }
  }

  def encodeToBase64(message: GeneratedMessage): Try[String] = {
    logger.debug(s"Attempting to encode protobuf message of type: ${message.getClass.getSimpleName}")
    logger.trace(s"Encoding message: $message")

    Try {
      val bytes = message.toByteArray
      logger.debug(s"Converted protobuf message to byte array of length: ${bytes.length}")

      val base64String = Base64.getEncoder.encodeToString(bytes)
      logger.info(s"Successfully encoded protobuf message to base64 string of length: ${base64String.length}")
      logger.trace(s"Encoded base64 string: $base64String")
      base64String
    }.recoverWith { case e: Exception =>
      logger.error(s"Failed to encode protobuf message to base64 string", e)
      Try(throw e)
    }
  }

  def jsonToProto[T <: GeneratedMessage](json: String)(implicit companion: scalapb.GeneratedMessageCompanion[T]): Try[T] = {
    logger.debug(s"Attempting to convert JSON to protobuf message of type: ${companion.scalaDescriptor.fullName}")
    logger.trace(s"Input JSON: $json")

    Try {
      val message = JsonFormat.fromJsonString[T](json)(companion)
      logger.info(s"Successfully converted JSON to protobuf message")
      logger.debug(s"Converted message: $message")
      message
    }.recoverWith { case e: Exception =>
      logger.error(s"Failed to convert JSON to protobuf message of type ${companion.scalaDescriptor.fullName}", e)
      Try(throw e)
    }
  }

  def protoToJson(message: GeneratedMessage): Try[String] = {
    logger.debug(s"Attempting to convert protobuf message of type ${message.getClass.getSimpleName} to JSON")
    logger.trace(s"Input message: $message")

    Try {
      val json = JsonFormat.toJsonString(message)
      logger.info(s"Successfully converted protobuf message to JSON of length: ${json.length}")
      logger.trace(s"Converted JSON: $json")
      json
    }.recoverWith { case e: Exception =>
      logger.error(s"Failed to convert protobuf message to JSON", e)
      Try(throw e)
    }
  }
}