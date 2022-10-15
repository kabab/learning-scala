import org.scalatest.funsuite.AnyFunSuite

class ParserTest extends AnyFunSuite {
  test("Parser.checkSyntax") {
    assert(CommandParser.parseCmd("get amine") == true)
    assert(CommandParser.parseCmd("get amine hello") == false)
    assert(CommandParser.parseCmd("get") == false)
    assert(CommandParser.parseCmd("something") == false)
    assert(CommandParser.parseCmd("set hello 2938 ") == true)
    assert(CommandParser.parseCmd("del hello 2938") == false)
  }
}
