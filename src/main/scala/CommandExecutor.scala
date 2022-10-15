import java.io.{BufferedInputStream, BufferedReader, File, FileInputStream, FileOutputStream, StringReader}
import java.nio.charset.StandardCharsets
import java.util.concurrent.{BlockingQueue, TimeUnit}
import scala.util.{Success, Try}

case class Store(file: File) {

  var store = new scala.collection.mutable.HashMap[String, String]

  def execute(command: Command): Option[String] = {
    append(command)
    process(command)
  }

  def process(cmd: Command): Option[String] = {
    cmd match {
      case GetCommand(label) => store.get(label)
      case SetCommand(label, value) => {
        store += (label -> value)
        Some(value)
      }
      case DelCommand(label) => {
        store -= label
        None
      }
    }
  }


  def append(command: Command) = {
    val output = new FileOutputStream(file, true)
    output.write((command.toString + "\n").getBytes(StandardCharsets.UTF_8))
  }

  def load() = {
    val stream = new BufferedInputStream(new FileInputStream(file))
    val bytes = stream.readAllBytes()
    val text = new String(bytes, StandardCharsets.UTF_8);
    val reader = new BufferedReader(new StringReader(text))

    reader.lines().forEach{
      CommandParser.parseCmd(_) match {
        case Success(value) => process(value)
        case error => println("Error when loading the file : " + error)
      }
    }

  }
}

case object Store {
  def apply(path: String): Store = {
    val file = new File(path + "/db.bin")
    if (!file.exists())
      file.createNewFile()
    val store = Store(file)
    store.load()
    store
  }
}

case class CommandWorker(
  requestQueue: BlockingQueue[Request],
  responseQueue: BlockingQueue[Response],
  store: Store
) extends Runnable {

  override def run(): Unit = {
    while(true) {
      val request = requestQueue.poll()
      if (request != null && request.payload.trim.length > 0) {
        println(s"new request with clientId ${request.sessionId} and payload ${request.payload}")
        CommandParser.parseCmd(request.payload) match {
          case Success(value) => store.execute(value) match {
            case Some(message) => responseQueue.put(PayloadResponse(message, request.sessionId))
            case None => responseQueue.put(EmptyResponse(request.sessionId))
          }
          case failure => responseQueue.put(PayloadResponse(failure.toString, request.sessionId))
        }
      }
    }
  }
}