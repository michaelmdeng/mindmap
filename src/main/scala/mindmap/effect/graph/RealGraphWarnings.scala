package mindmap.effect.graph

import cats.Applicative
import cats.instances.list._
import cats.syntax.applicative._
import cats.syntax.functorFilter._
import scalax.collection.Graph
import scalax.collection.GraphEdge.DiEdge
import scalax.collection.GraphTraversal.AnyConnected

import mindmap.model.Entity
import mindmap.model.Note
import mindmap.model.Tag
import mindmap.model.graph.GraphWarningAlgebra

class RealGraphWarnings[F[+_]: Applicative[?[_]]]
    extends GraphWarningAlgebra[F] {
  def warnings(graph: Graph[Entity, DiEdge]): F[Iterable[String]] = {
    val tagWarnings = graph.nodes.toList
      .mapFilter(node => {
        node.toOuter match {
          case tag: Tag if node.degree <= 1 => Some(tag)
          case _ => None
        }
      })
      .map(tag => f"Tag: ${tag.name} only has one linked note")

    val noteWarnings = graph.nodes.toList
      .mapFilter(node => {
        node.toOuter match {
          case note: Note if node.degree == 0 => Some(note)
          case _ => None
        }
      })
      .map(note => {
        f"Note: ${note.title} is not linked to any other notes or tags"
      })

    val tagOverlapWarnings = graph.nodes
      .filter(node => {
        node.toOuter match {
          case tag: Tag => true
          case _ => false
        }
      })
      .flatMap(node => {
        val tag = node.toOuter.asInstanceOf[Tag]
        val connectedTags = node
          .withMaxDepth(2)
          .withDirection(AnyConnected)
          .filter(connectedNode => {
            connectedNode.toOuter match {
              case connectedTag: Tag if connectedTag != tag => true
              case _ => false
            }
          })

        val connectedNotes = node.neighbors.filter(neighbor => {
          neighbor.toOuter match {
            case note: Note => true
            case _ => false
          }
        })

        connectedTags.toList.mapFilter(connectedTag => {
          if (connectedNotes
              .filter(note => !connectedTag.neighbors.contains(note))
              .isEmpty) {
            val overlappedTag = connectedTag.toOuter.asInstanceOf[Tag].name
            Some(
              f"Tag: ${tag.name} shares all notes with tag: ${overlappedTag}"
            )
          } else {
            None
          }
        })
      })

    (tagWarnings ++ noteWarnings ++ tagOverlapWarnings).pure[F]
  }
}
