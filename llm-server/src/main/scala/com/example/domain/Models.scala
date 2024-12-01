package com.example.domain

case class LlmRequest(
                       inputText: String,
                       temperature: Option[Double] = None,
                       maxTokens: Option[Int] = None
                     )

case class RequestContext(
                           requestId: String
                         )

case class ApiGatewayRequest(
                              queryStringParameters: Map[String, String],
                              requestContext: RequestContext
                            )

case class ApiGatewayResponse(
                               statusCode: Int,
                               headers: Map[String, String],
                               body: String
                             )

case class LlmResponse(
                        response: String
                      )

case class ErrorResponse(
                          error: String
                        )