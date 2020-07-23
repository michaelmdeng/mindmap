package mindmap.model.parser.markdown

import scala.math.max
import scala.math.min

import mindmap.model.Tag

sealed trait Block
sealed trait Paragraph

case class TagBlock(tags: List[Tag]) extends Block with Paragraph
case class CodeBlock(code: String, language: Option[String])
    extends Block
    with Paragraph
case class BlockQuote(text: String) extends Block with Paragraph
case class Header(text: String, level: Int) extends Block with Paragraph

object Header {
  def apply(text: String, level: Int) = new Header(text, min(max(level, 1), 6))
}

case class Line(text: String) extends Block

case class TextParagraph(text: String) extends Paragraph
