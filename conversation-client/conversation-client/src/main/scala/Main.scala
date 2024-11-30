import com.typesafe.scalalogging.LazyLogging
import services.ConversationalAgent

object Main extends App with LazyLogging {
  val agent = new ConversationalAgent()

  if (args.length < 1) {
    logger.error("Please provide an initial query as a command line argument")
    System.exit(1)
  }

  try {
    // Join all arguments to handle quotes properly
    val initialQuery = args.mkString(" ").trim.stripPrefix("'").stripSuffix("'")
    logger.info(s"Starting conversation with initial query: $initialQuery")

    val conversation = agent.runConversation(initialQuery)

    // Save conversation to CSV
    agent.saveConversationToCSV(conversation, "conversation_results.csv")

    // Print statistics
    logger.info(s"Conversation completed with ${conversation.length} turns")
    logger.info(s"Average processing time: ${conversation.map(_.processingTimeMs).sum / conversation.length}ms")

  } catch {
    case e: Exception =>
      logger.error(s"Error during conversation: ${e.getMessage}")
      System.exit(1)
  }
}