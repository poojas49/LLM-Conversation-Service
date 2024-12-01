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

object Server extends App with LazyLogging {
  logger.info("Initializing Conversation System...")

  implicit val system: ActorSystem[Nothing] = {
    logger.debug("Creating ActorSystem...")
    ActorSystem(Behaviors.empty, "ConversationSystem")
  }
  implicit val executionContext: ExecutionContextExecutor = system.executionContext

  logger.info("Initializing ConversationalAgent and Routes...")
  val conversationalAgent = new ConversationalAgent()
  val routes = new ConversationRoutes(conversationalAgent)

  val shutdownPromise = Promise[Boolean]()

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