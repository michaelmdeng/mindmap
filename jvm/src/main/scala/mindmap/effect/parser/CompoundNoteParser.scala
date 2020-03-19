package mindmap.effect.parser

import cats.data.Chain
import cats.Monad
import cats.implicits._
import java.time.LocalDateTime

import mindmap.model._
import mindmap.model.parser._

class CompoundNoteParser[F[_]: Monad[?[_]]](
  contentParser: ContentParserAlgebra[F],
  metadataParser: MetadataParserAlgebra[F],
  linkParser: LinkParserAlgebra[F],
  tagParser: TagParserAlgebra[F]
) extends NoteParserAlgebra[F] {
  def content(): F[String] = contentParser.content()

  def createDate(): F[LocalDateTime] = metadataParser.createDate()
  def id(): F[String] = metadataParser.id()
  def modifiedDate(): F[LocalDateTime] = metadataParser.modifiedDate()

  def links(): F[Chain[Link]] = linkParser.links()

  def tags(): F[Set[Tag]] = tagParser.tags()

  def parseNote(): F[Note] =
    for {
      c <- content()
      cd <- createDate()
      md <- modifiedDate()
      i <- id()
      ls <- links()
      ts <- tags()
    } yield {
      Note(
        content = c,
        createDate = cd,
        id = i,
        modifiedDate = md,
        links = ls,
        tags = ts
      )
    }
}
