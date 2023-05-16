package mindmap.model.parser.markdown

import scala.util.parsing.combinator.RegexParsers

import mindmap.model.Strings._
import mindmap.model.Tag
import mindmap.model.parser.SharedParsers

class BlockParsers extends SharedParsers {
  override def skipWhitespace: Boolean = false

  def tagSeparator: Parser[String] = literal(":")
  def tags: Parser[TagBlock] =
    for {
      _ <- spaces(0, 3)
      _ <- tagSeparator
      tags <- rep1(regex("[^:\n]+:".r)).map(ts => ts.map(s => Tag(s.init)))
    } yield (TagBlock(tags))

  def header: Parser[Header] =
    for {
      _ <- spaces(0, 3)
      level <- regex("#+".r).map(_.size)
      _ <- optWhitespace
      text <- chainl1(
        regex("[^#\t \n]+".r),
        optWhitespace.map(space => { (left: String, right: String) =>
          left + space + right
        })
      )
      _ <- optWhitespace
      _ <- opt(regex("#+".r).map(_.size))
    } yield (Header(text, level))

  private def blockQuoteOpen: Parser[String] = regex(">[ \t]*".r)
  def blockQuote: Parser[BlockQuote] =
    for {
      _ <- blockQuoteOpen
      lines <- rep1sep(regex("[^\n]*".r), lineBreak ~ blockQuoteOpen)
    } yield {
      BlockQuote(lines.map(_.stripTrailingWhitespace).mkString("\n"))
    }

  def codeBlock: Parser[CodeBlock] =
    for {
      _ <- spaces(0, 3)
      _ <- literal("```")
      _ <- optWhitespace
      language <- opt(regex("[^\n]+".r)).map(opt =>
        opt.map(_.stripTrailingWhitespace)
      )
      code <- regex("[^`]+".r).map(s =>
        s.stripPrecedingLineBreaks.stripTrailingLineBreaks
      )
      _ <- literal("```")
    } yield (CodeBlock(code, language))

  def line: Parser[Line] = regex("[^\n]*".r).map(Line(_))

  def blocks: Parser[List[Block]] =
    phrase(
      rep1sep[Block](
        tags | codeBlock | header | blockQuote | line,
        lineBreak
      )
    )

  def mergeBlocks(blocks: List[Block]): List[Paragraph] = {
    blocks.foldLeft(List[Paragraph]())((acc, block) => {
      block match {
        case t: TagBlock => acc :+ t
        case c: CodeBlock => acc :+ c
        case b: BlockQuote => acc :+ b
        case h: Header => acc :+ h
        case lb: LineBreak.type => acc :+ lb
        case Line(s) => {
          if (s.nonEmpty) {
            acc.lastOption match {
              case Some(TextParagraph(p)) =>
                acc.updated(acc.length - 1, TextParagraph(p + "\n" + s))
              case _ => acc :+ TextParagraph(s)
            }
          } else {
            acc :+ LineBreak
          }
        }
      }
    })
  }
}
