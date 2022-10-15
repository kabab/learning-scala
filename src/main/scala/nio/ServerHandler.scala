package nio

import java.nio.ByteBuffer

trait ServerHandler {
  def onAccept()
  def onRead(buff: ByteBuffer)
  def onClose()
}

object EmptyHandler extends ServerHandler {
  override def onAccept(): Unit = ???

  override def onRead(buff: ByteBuffer): Unit = ???

  override def onClose(): Unit = ???
}