package mindmap.effect.parser

import cats.data.Chain
import cats.Applicative
import cats.implicits._

import mindmap.model.parser.LinkParserAlgebra
import mindmap.model.Link

class MarkdownLinkParser[F[_]: Applicative[?[_]]](content: F[String])
    extends LinkParserAlgebra[F] {
  def links(): F[Chain[Link]] =
    for {
      c <- content
    } yield {
      val pattern = "\\[([^\\]]+)\\]\\(([^\\)]+)\\)".r
      val matches = pattern.findAllMatchIn(c)

      val links = matches
        .filter(_.groupCount >= 2)
        .map(m => (m.group(1), m.group(2)))
        .map { case (text, link) => link }
        .toSeq

      Chain.fromSeq(links)
    }
}
