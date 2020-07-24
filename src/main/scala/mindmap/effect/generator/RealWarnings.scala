package mindmap.effect.generator

import cats.Monad
import cats.implicits._

import mindmap.model.ResolvedLink
import mindmap.model.generator.ZettelkastenWarningAlgebra
import mindmap.model.Note
import mindmap.model.Zettelkasten
import mindmap.model.Tag

class RealWarnings[F[+_]: Monad[?[_]]](val zettel: Zettelkasten)
    extends ZettelkastenWarningAlgebra[F] {
  private def lonelyTags: F[Iterable[String]] = {
    zettel.links
      .mapFilter(link => {
        (link.from match {
          case (note: Note) => None
          case (tag: Tag) => Some(tag)
        })
      })
      .groupBy(t => t)
      .view
      .mapValues(_.size)
      .filter {
        case (_, linkCount) => linkCount == 1
      }
      .toMap
      .map {
        case (tag, _) => f"Tag: ${tag.name} only has one linked note"
      }
      .pure[F]
  }

  private def lonelyNotes: F[Iterable[String]] = {
    val noteLinks = zettel.links
      .mapFilter(link => {
        (link.from, link.to) match {
          case (n1: Note, n2: Note) => Some(Seq(n1, n2))
          case (n: Note, _) => Some(Seq(n))
          case (_, n: Note) => Some(Seq(n))
          case (_, _) => None
        }
      })
      .flatten

    zettel.notes
      .filter(note => !noteLinks.contains(note))
      .map(note => {
        f"Note: ${note.title} is not linked to any other notes or tags"
      })
      .pure[F]
  }

  def warnings: F[Iterable[String]] =
    for {
      tagWarnings <- lonelyTags
      noteWarnings <- lonelyNotes
    } yield (tagWarnings ++ noteWarnings)
}
