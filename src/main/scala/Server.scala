import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.{SelectableChannel, SelectionKey, Selector, ServerSocketChannel, SocketChannel}
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.{LinkedBlockingDeque, TimeUnit}
import scala.collection.mutable
import scala.util.Success

class Server(host: String, port: Int) {
  val selector: Selector = Selector.open()
  val clientSelector: Selector = Selector.open()
  val requestsQueue: LinkedBlockingDeque[Request] = new LinkedBlockingDeque[Request]()
  val responseQueue: LinkedBlockingDeque[Response] = new LinkedBlockingDeque[Response]()

  val clientChannels: scala.collection.mutable.Map[String, SocketChannel] =
    new mutable.HashMap

  val serverChannel = createServerSocket()
  var channels = scala.collection.mutable.Map

  def run(): Unit = {
    val store = Store("/tmp/")

    val requestProcessor = new Thread(CommandWorker(requestsQueue, responseQueue, store))
    requestProcessor.start()

    serverChannel.register(selector, SelectionKey.OP_ACCEPT)
    while(true) {
      acceptConnections()
      pollMessages()
      sendResponses()
    }

    requestProcessor.join()
  }

  def sendResponses() = {
    val response = responseQueue.poll()
    if (response != null) {
      println(s"new response for sessionId $response")
      response match {
        case PayloadResponse(payload, sessionId) => sendResponseToClient(payload, sessionId)
        case EmptyResponse(_) => sendResponseToClient("Done", _)
      }
    }
  }

  def sendResponseToClient(payload: String, sessionId: String): Unit = {
    clientChannels.get(sessionId) match {
      case Some(channel) =>
        val buffer = ByteBuffer.wrap(payload.getBytes())
        try channel.write(buffer)
      case None => println("Error: client not found when sending response")
    }
  }

  def acceptConnections() = {
    val ready = selector.select(100)
    if (ready > 0) {
      val keys = selector.selectedKeys()
      val iter = keys.iterator()
      while (iter.hasNext) {
        val key = iter.next()
        iter.remove()
        if (key.isAcceptable) {
          val uuid: UUID = UUID.randomUUID()
          val channel = key.channel().asInstanceOf[ServerSocketChannel].accept()
          channel.configureBlocking(false)
          channel.socket().setKeepAlive(true)
          channel.register(clientSelector, SelectionKey.OP_READ, uuid)
          clientChannels.put(uuid.toString, channel)
        }
      }
    }
  }

  def pollMessages() = {
    val clientReady = clientSelector.select(500)
    if (clientReady > 0) {
      val keys = clientSelector.selectedKeys()
      val iter = keys.iterator()
      while (iter.hasNext) {
        val key = iter.next()
        iter.remove()
        if (key.isReadable) {
          val buffer = ByteBuffer.allocate(1024)
          val channel = key.channel().asInstanceOf[SocketChannel]
          try {
            val read = channel.read(buffer)
            if ( read >= 0) {
              val message = new String(buffer.array(), StandardCharsets.UTF_8).substring(0, read)
              message.split("\n")
                .map(_.trim)
                .map(Request(_, key.attachment().toString))
                .foreach{
                  requestsQueue.add(_)
                }
            } else {
              clientChannels.remove(key.attachment().toString)
              channel.close()
            }
          } catch {
            case ioe: IOException =>
              println("Error reading from the client")
          }
        }
      }
    }
  }

  def createServerSocket(): ServerSocketChannel = {
    val channel = ServerSocketChannel.open()
    channel.configureBlocking(false)
    val addressSock = new InetSocketAddress(host, port)
    channel.socket().bind(addressSock)
    channel

  }

}

case class Request(val payload: String, val sessionId: String)

sealed trait Response

case class PayloadResponse(val payload: String, val sessionId: String) extends Response
case class EmptyResponse(val sessionId: String) extends Response

