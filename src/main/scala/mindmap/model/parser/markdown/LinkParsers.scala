package mindmap.model.parser.markdown

import scala.util.parsing.combinator.RegexParsers

import mindmap.model.Strings._
import mindmap.model.UnresolvedLink

class LinkParsers extends SharedParsers {
  def link: Parser[UnresolvedLink] =
    for {
      _ <- literal("[")
      _ <- regex("[^\\]]+".r)
      _ <- literal("](")
      to <- regex("[^\\)]+".r)
      _ <- literal(")")
    } yield (UnresolvedLink(to))

  def links: Parser[List[UnresolvedLink]] =
    for {
      _ <- regex("[^\\[]*".r)
      ls <- repsep(link, regex("[^\\[]*".r))
    } yield (ls)
}

object LinkParsers {
  def main(args: Array[String]): Unit = {
    val content = scala.io.Source
      .fromFile("/home/mdeng/Dropbox/vimwiki/wiki/backpressure.md")
      .getLines
      .mkString("\n")

    val blockParsers = new BlockParsers()
    val blocks = blockParsers.parse(blockParsers.blocks, content).get
    println(blocks)
    val paragraphs = blockParsers.mergeBlocks(blocks)
    println(paragraphs)
    val parsers = new LinkParsers()
    val link = "[foo](bar)"
    val linkParagraph =
      "The most popular of these is the [GNU General Public License (GPL)](gpl)."

    // println(parsers.parse(parsers.link, link))
    // println(parsers.parse(parsers.links, link))
    val paragraph = paragraphs.last.asInstanceOf[TextParagraph]
    println(parsers.parse(parsers.links, paragraph.text))
  }
}
