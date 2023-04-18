package mindmap.model.graph

import cats.Show
import scalax.collection.Graph
import scalax.collection.GraphEdge.DiEdge

import mindmap.model.Entity
import mindmap.model.Note
import mindmap.model.Tag

trait GraphWarningAlgebra[F[_]] {
  def warnings(graph: Graph[Entity, DiEdge]): F[Iterable[GraphWarning]]
}

sealed trait GraphWarning
case class SingleTag(tag: Tag) extends GraphWarning
case class SingleNote(note: Note) extends GraphWarning
case class OverlappingTags(first: Tag, second: Tag) extends GraphWarning

object GraphWarning {
  object instances {
    implicit def showForSingleTag: Show[SingleTag] = new Show[SingleTag] {
      def show(singleTag: SingleTag): String =
        f"Tag: ${singleTag.tag.name} only has one linked note"
    }

    implicit def orderingForSingleTag: Ordering[SingleTag] =
      Ordering.by(_.tag.name)

    implicit def showForSingleNote: Show[SingleNote] = new Show[SingleNote] {
      def show(singleNote: SingleNote): String =
        f"Note: ${singleNote.note.title} is not linked to any other notes or tags"
    }

    implicit def orderingForSingleNote: Ordering[SingleNote] =
      Ordering.by(_.note.title)

    implicit def showForOverlappingTags: Show[OverlappingTags] =
      new Show[OverlappingTags] {
        def show(tags: OverlappingTags): String =
          f"Tag: ${tags.first.name} shares all notes with tag: ${tags.second.name}"
      }

    implicit def orderingForOverlappingTags: Ordering[OverlappingTags] =
      Ordering.by(t => {
        t match {
          case OverlappingTags(first, second) =>
            List(first.name, second.name).sorted.head
        }
      })

    implicit def showForWarning: Show[GraphWarning] =
      new Show[GraphWarning] {
        def show(warning: GraphWarning): String = {
          warning match {
            case singleTag: SingleTag => showForSingleTag.show(singleTag)
            case singleNote: SingleNote => showForSingleNote.show(singleNote)
            case overlappingTags: OverlappingTags =>
              showForOverlappingTags.show(overlappingTags)
          }
        }
      }
  }
}
