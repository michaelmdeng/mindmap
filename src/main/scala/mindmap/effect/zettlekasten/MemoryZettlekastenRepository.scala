package mindmap.effect.zettelkasten

import cats.Monad
import cats.syntax.applicative._
import cats.syntax.functor._
import cats.syntax.flatMap._

import mindmap.model.Note
import mindmap.model.ResolvedLink
import mindmap.model.Tag
import mindmap.model.Zettelkasten
import mindmap.model.ZettelkastenAlgebra

class MemoryZettelkastenRepository[F[+_]: Monad[*[_]]](
  zettelkasten: Zettelkasten
) extends ZettelkastenAlgebra[F] {
  def getNote(title: String): F[Option[Note]] =
    zettelkasten.notes.find(_.title == title).pure[F]

  def getTag(name: String): F[Option[Tag]] =
    zettelkasten.tags.find(_.name == name).pure[F]

  def getTagNotes(tag: Tag): F[Set[Note]] =
    for {
      links <- zettelkasten.links
        .filter(link => {
          link.from match {
            case t: Tag => t.name == tag.name
            case default => false
          }
        })
        .pure[F]
      notes <- links
        .flatMap(link => {
          link.to match {
            case n: Note => List(n)
            case default => List()
          }
        })
        .sortBy(_.title)
        .toSet
        .pure[F]
    } yield (notes)

  def notes(): F[Iterable[Note]] = zettelkasten.notes.pure[F]

  def tags(): F[Set[Tag]] = zettelkasten.tags.toSet.pure[F]

  def findNote(id: Long): F[Option[Note]] = zettelkasten.findNote(id).pure[F]

  def findTag(id: Long): F[Option[Tag]] = zettelkasten.findTag(id).pure[F]
}
