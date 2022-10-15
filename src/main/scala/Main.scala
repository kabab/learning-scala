import nio.{Logger, ServerHandler, SigmaServer}

import java.nio.ByteBuffer
object Main extends Logger {

  def main(args: Array[String]): Unit = {

    val server = SigmaServer("0.0.0.0", 3080, new ServerHandler {
      override def onAccept(): Unit = {
        logger.info("Test")
      }

      override def onRead(buff: ByteBuffer): Unit = ???

      override def onClose(): Unit = ???
    })
    server.start
  }
}