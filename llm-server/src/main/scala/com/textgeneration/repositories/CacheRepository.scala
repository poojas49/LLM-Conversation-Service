package com.textgeneration.repositories

import com.textgeneration.models._
import scala.collection.concurrent.TrieMap
import java.time.Instant
import org.slf4j.LoggerFactory

class CacheRepository(maxSize: Int = 1000, ttlSeconds: Long = 3600) {
  private val logger = LoggerFactory.getLogger(getClass)
  private val cache = TrieMap[String, CacheEntry]()

  def get(key: String): Option[GenerationResponse] = {
    cache.get(key).flatMap { entry =>
      if (isExpired(entry)) {
        logger.debug(s"Cache entry expired for key: $key")
        cache.remove(key)
        None
      } else {
        Some(entry.response)
      }
    }
  }

  def put(key: String, response: GenerationResponse): Unit = {
    cleanup()
    cache.put(key, CacheEntry(response, Instant.now().getEpochSecond))
    logger.debug(s"Added new cache entry for key: $key")
  }

  private def isExpired(entry: CacheEntry): Boolean = {
    val now = Instant.now().getEpochSecond
    now - entry.timestamp > ttlSeconds
  }

  private def cleanup(): Unit = {
    if (cache.size >= maxSize) {
      logger.debug("Starting cache cleanup")
      val expired = cache.filter { case (_, entry) => isExpired(entry) }
      expired.keys.foreach(cache.remove)

      if (cache.size >= maxSize) {
        val oldest = cache.toList.sortBy(_._2.timestamp).take(cache.size - maxSize + 1)
        oldest.foreach { case (key, _) => cache.remove(key) }
        logger.debug(s"Removed ${oldest.size} oldest entries from cache")
      }
    }
  }
}