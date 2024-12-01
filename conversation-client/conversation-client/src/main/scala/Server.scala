import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import api.ConversationRoutes
import services.ConversationalAgent
import com.typesafe.scalalogging.LazyLogging
import scala.util.{Success, Failure}
import scala.concurrent.{ExecutionContextExecutor, Promise}
import scala.concurrent.duration._
import config.AppConfig


/**
 * Core System Architecture Overview
 *
 * This system implements a conversational AI service that combines two LLM services:
 * 1. A cloud-based service for primary response generation
 * 2. Ollama (a local LLM) for follow-up query generation
 *
 * The architecture follows these key design principles:
 * - Separation of concerns (routing, business logic, configuration)
 * - Immutable data models
 * - Configurable parameters via HOCON
 * - Comprehensive logging for debugging and monitoring
 * - Graceful error handling and shutdown
 * - Asynchronous processing where appropriate
 */

/**
 * Main Server Component
 *
 * Responsible for:
 * - Initializing the Akka actor system
 * - Setting up HTTP routes
 * - Managing server lifecycle
 * - Handling graceful shutdown
 *
 * Design Rationale:
 * - Uses Akka HTTP for robust HTTP handling
 * - Implements graceful shutdown via Promise for clean termination
 * - Separates configuration from implementation
 * - Uses LazyLogging for performance
 */
object Server extends App with LazyLogging {
  logger.info("Initializing Conversation System...")

  implicit val system: ActorSystem[Nothing] = {
    logger.debug("Creating ActorSystem...")
    ActorSystem(Behaviors.empty, "ConversationSystem")
  }

  // Essential for Akka's asynchronous operations
  implicit val executionContext: ExecutionContextExecutor = system.executionContext

  // Initialize core components
  logger.info("Initializing ConversationalAgent and Routes...")
  val conversationalAgent = new ConversationalAgent()
  val routes = new ConversationRoutes(conversationalAgent)

  // Shutdown promise ensures clean termination
  val shutdownPromise = Promise[Boolean]()

  // Server binding with configuration-driven parameters
  logger.info(s"Starting server on ${AppConfig.Server.host}:${AppConfig.Server.port}")
  val serverBinding = Http().newServerAt(AppConfig.Server.host, AppConfig.Server.port).bind(routes.routes)

  serverBinding.onComplete {
    case Success(binding) =>
      val address = binding.localAddress
      logger.info(s"Server successfully started at http://${address.getHostString}:${address.getPort}/")
      logger.debug("Registering shutdown hook...")

      sys.addShutdownHook {
        logger.info("Shutdown initiated...")
        binding.terminate(AppConfig.Server.terminationTimeoutSeconds.seconds).onComplete { _ =>
          logger.info("Server binding terminated")
          system.terminate()
          logger.info("Actor system terminated")
          shutdownPromise.success(true)
          logger.info("Shutdown completed successfully")
        }
      }

    case Failure(ex) =>
      logger.error(s"Failed to bind server to ${AppConfig.Server.host}:${AppConfig.Server.port}", ex)
      logger.info("Initiating emergency shutdown...")
      system.terminate()
      shutdownPromise.success(false)
  }

  logger.info("Server is ready to accept connections. Press ENTER to stop.")
  scala.io.StdIn.readLine()

  logger.info("Shutdown signal received, initiating graceful shutdown...")
  shutdownPromise.future.map { success =>
    if (success) {
      logger.info("Server shutdown completed successfully")
    } else {
      logger.warn("Server shutdown completed with potential errors")
    }
    system.terminate()
  }
}