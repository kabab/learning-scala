import java.util.concurrent.{BlockingQueue, TimeUnit}
import scala.util.{Success, Try}
import scala.util.parsing.combinator._

sealed trait Command

case class GetCommand(val label: String) extends Command {
  override def toString: String = s"get $label"
}
case class DelCommand(val label: String) extends Command {
  override def toString: String = s"del $label"
}
case class SetCommand(val label: String, val value: String) extends Command {
  override def toString: String = s"set $label $value"
}

case class CommandParserException(val msg: String) extends Throwable(msg)
case object CommandParser extends RegexParsers {

  def label: Parser[String] = """[a-zA-Z]+[A-Za-z0-9]*""".r ^^ { _.toString }
  def value: Parser[String] = """[a-zA-Z0-9]+""".r ^^ { _.toString }
  def eol: Parser[String] = """$""".r ^^ {_.toString}
  def getCmd: Parser[GetCommand] = "get" ~ label <~ eol ^^ { case _ ~ lbl => GetCommand(lbl) }
  def setCmd: Parser[SetCommand] = "set" ~ label ~ value <~ eol ^^ { case _ ~ lbl ~ vl => SetCommand(lbl, vl) }
  def delCmd: Parser[DelCommand] = "del" ~ label <~eol ^^ { case _ ~ lbl  => DelCommand(lbl) }

  def expr: Parser[Command] = getCmd | setCmd | delCmd

  def parseCmd(command: String): Try[Command] =
    parse(expr, command) match {
      case Success(cmd, _) => scala.util.Success(cmd)
      case failure => scala.util.Failure(CommandParserException(failure.toString))
    }
}
