package mindmap.model.parser

import scala.util.parsing.combinator.RegexParsers

trait SharedParsers extends RegexParsers {
  def spaces(from: Int, to: Int): Parser[String] =
    regex(f"[ ]{${from},${to}}".r)
  def nonEmptyLine: Parser[String] = regex(".+\n".r)
  def whitespace: Parser[String] = regex("[\t ]+".r)
  def optWhitespace: Parser[String] = regex("[\t ]*".r)
  def lineBreak: Parser[String] = literal("\n")
}
