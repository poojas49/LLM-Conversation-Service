package server

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import api.ConversationRoutes
import services.ConversationalAgent
import com.typesafe.scalalogging.LazyLogging
import scala.util.{Success, Failure}
import scala.concurrent.{ExecutionContextExecutor, Promise}
import scala.concurrent.duration._

object Server extends App with LazyLogging {
  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "ConversationSystem")
  implicit val executionContext: ExecutionContextExecutor = system.executionContext

  val conversationalAgent = new ConversationalAgent()
  val routes = new ConversationRoutes(conversationalAgent)

  val host = "0.0.0.0"
  val port = 8081

  val shutdownPromise = Promise[Boolean]()

  val serverBinding = Http().newServerAt(host, port).bind(routes.routes)

  serverBinding.onComplete {
    case Success(binding) =>
      val address = binding.localAddress
      logger.info(s"Server online at http://${address.getHostString}:${address.getPort}/")

      sys.addShutdownHook {
        binding.terminate(10.seconds).onComplete { _ =>
          system.terminate()
          shutdownPromise.success(true)
        }
      }

    case Failure(ex) =>
      logger.error(s"Failed to bind server: ${ex.getMessage}")
      system.terminate()
      shutdownPromise.success(false)
  }

  // Keep the server running
  scala.io.StdIn.readLine() // This will keep the server running until you press Enter
  shutdownPromise.future.map { _ => system.terminate() }
}