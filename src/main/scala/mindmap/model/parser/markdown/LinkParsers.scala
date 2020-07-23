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
