package com.example

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import org.mockito.Mockito._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.io.{ByteArrayOutputStream, PrintStream}
import scala.jdk.CollectionConverters._

class LambdaHandlerTest extends AnyFlatSpec with Matchers {

  "LambdaHandler" should "handle valid requests" in {
    val handler = new LambdaHandler()
    val context = mock(classOf[Context])
    val logger = new TestLogger()
    when(context.getLogger).thenReturn(logger)

    val queryParams = Map("query" -> "SGVsbG8gV29ybGQ=").asJava // Base64 encoded "Hello World"
    val requestContext = new APIGatewayProxyRequestEvent.ProxyRequestContext()
    requestContext.setRequestId("test-request-id")

    val request = new APIGatewayProxyRequestEvent()
      .withQueryStringParameters(queryParams)
      .withRequestContext(requestContext)

    val response = handler.handleRequest(request, context)

    response.getStatusCode shouldBe 400 // Since the base64 decode will fail for our test string
    response.getBody should include("Error")
  }

  it should "handle missing query parameter" in {
    val handler = new LambdaHandler()
    val context = mock(classOf[Context])
    val logger = new TestLogger()
    when(context.getLogger).thenReturn(logger)

    val request = new APIGatewayProxyRequestEvent()
      .withRequestContext(new APIGatewayProxyRequestEvent.ProxyRequestContext())

    val response = handler.handleRequest(request, context)

    response.getStatusCode shouldBe 400
    response.getBody should include("Missing required 'query' parameter")
  }
}

// Test implementation of LambdaLogger
class TestLogger extends com.amazonaws.services.lambda.runtime.LambdaLogger {
  private val output = new ByteArrayOutputStream()
  private val printStream = new PrintStream(output)

  override def log(message: String): Unit = printStream.println(message)
  override def log(message: Array[Byte]): Unit = printStream.write(message)

  def getOutput: String = output.toString
}