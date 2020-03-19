package mindmap.effect.parser

import cats.Applicative
import cats.implicits._

import mindmap.model.Tag
import mindmap.model.parser.TagParserAlgebra

class VimwikiTagParser[F[_]: Applicative[?[_]]](content: F[String])
    extends TagParserAlgebra[F] {
  def tags(): F[Set[Tag]] =
    for {
      c <- content
    } yield {
      val firstLine = c.takeWhile(_ != '\n')
      val pattern = "^:[\\w\\-:]+:$".r
      pattern.findFirstIn(firstLine) match {
        case Some(s) => s.split(':').filter(_.nonEmpty).toSet
        case None => Set()
      }
    }
}
