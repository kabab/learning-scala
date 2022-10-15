package nio

import org.slf4j.event.Level

import java.net.InetSocketAddress
import java.nio.channels.{SelectableChannel, SelectionKey, Selector, ServerSocketChannel, SocketChannel}
import java.util.concurrent.atomic.AtomicBoolean


sealed trait Server extends Logger {
  def start
  def shutdown
}

case class SigmaServer(host: String, port: Int, handler: ServerHandler = EmptyHandler ) extends Server {

  var started = new AtomicBoolean(false)
  val serverChannel = createServerSocket()
  val selector = Selector.open()
  val clientSelector = Selector.open()

  val acceptThread = new Thread {
    override def run {
      while(started.get()) acceptConnection()
    }
  }

  val readThread = new Thread {
    override def run: Unit = {
      while(started.get()) readLoop()
    }
  }

  def createServerSocket(): ServerSocketChannel = {
    val channel = ServerSocketChannel.open()
    channel.configureBlocking(false)
    val addressSock = new InetSocketAddress(host, port)
    channel.socket().bind(addressSock)
    channel
  }

  def readLoop(): Unit = {
    val ready = selector.select(100)
    if(ready > 0) {
      val keys = selector.selectedKeys()
      val iter = keys.iterator()
      while (iter.hasNext) {
        val key = iter.next()
        if (key.isReadable) {

        }
      }
    }
  }

  def acceptConnection(): Unit = {
    val ready = selector.select(100)

    if (ready > 0) {
      val keys = selector.selectedKeys()
      val iter = keys.iterator()
      while (iter.hasNext) {
        val key = iter.next()
        iter.remove()
        if (key.isAcceptable) {
          AcceptAndConfigureChannel(key.channel())
          // clientChannels.put(uuid.toString, channel)
        }
      }
    }
  }

  def AcceptAndConfigureChannel(sChannel: SelectableChannel): SocketChannel = {
    val socketChannel = sChannel.asInstanceOf[ServerSocketChannel].accept()
    socketChannel.configureBlocking(false)
    socketChannel.socket().setKeepAlive(true)
    socketChannel.register(clientSelector, SelectionKey.OP_READ)
    handler.onAccept()
    socketChannel
  }

  override def start(): Unit = {
    if (started.get()) throw ServerAlreadyStartedException("Server already running")
    serverChannel.register(selector, SelectionKey.OP_ACCEPT)
    started.set(true)
    acceptThread.start()
    readThread.start()
    readThread.join()
    acceptThread.join()
  }

  override def shutdown: Unit = {
    started.set(false)
    serverChannel.close()
  }
}

case class ServerAlreadyStartedException(msg: String) extends Exception(msg)