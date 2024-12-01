package com.example.domain


/**
 * Domain Models
 * ============
 *
 * Defines the core domain models used throughout the application for
 * request/response handling and data transfer.
 *
 * Design Rationale:
 * ----------------
 * - Immutable case classes for type safety
 * - Clear separation of concerns
 * - Optional parameters where appropriate
 * - Consistent naming conventions
 *
 * Model Descriptions:
 * -----------------
 * LlmRequest:
 *   - Primary input model
 *   - Contains generation parameters
 *   - Optional temperature and token limits
 *
 * RequestContext:
 *   - Request tracking information
 *   - Unique request identification
 *
 * ApiGatewayRequest:
 *   - Integration model for API Gateway
 *   - Contains query parameters and context
 *
 * ApiGatewayResponse:
 *   - API Gateway response wrapper
 *   - Includes status and headers
 *
 * LlmResponse:
 *   - Generated text container
 *   - Simple response structure
 *
 * ErrorResponse:
 *   - Standard error message format
 *   - Used for all error scenarios
 */
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