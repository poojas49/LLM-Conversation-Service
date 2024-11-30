package com.textgeneration.services

import com.textgeneration.models._
import com.textgeneration.repositories.CacheRepository
import com.textgeneration.clients.LambdaClient
import scala.concurrent.{Future, ExecutionContext}
import org.slf4j.LoggerFactory

class TextGenerationService(
                             cacheRepository: CacheRepository,
                             lambdaClient: LambdaClient
                           )(implicit ec: ExecutionContext) {

  private val logger = LoggerFactory.getLogger(getClass)

  def generateText(request: GenerationRequest): Future[GenerationResponse] = {
    val processedQuery = preprocessQuery(request.query)

    cacheRepository.get(processedQuery) match {
      case Some(cachedResponse) =>
        logger.info(s"Cache hit for query: $processedQuery")
        Future.successful(cachedResponse)

      case None =>
        logger.info(s"Cache miss for query: $processedQuery")
        for {
          response <- lambdaClient.generateText(request)
          _ <- Future.successful(cacheRepository.put(processedQuery, response))
        } yield response
    }
  }

  private def preprocessQuery(query: String): String = {
    query.trim.toLowerCase
  }
}