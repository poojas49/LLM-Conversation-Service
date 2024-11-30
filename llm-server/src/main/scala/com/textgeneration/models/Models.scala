package com.textgeneration.models

case class GenerationRequest(
                              query: String,
                              max_length: Int = 150,
                              temperature: Double = 0.7
                            )

case class ResponseMetadata(
                             length: Int,
                             stop_reason: String,
                             processing_time_ms: Long,
                             model: String
                           )

case class GenerationResponse(
                               response: String,
                               metadata: ResponseMetadata
                             )