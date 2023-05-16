package mindmap.effect.graph

import cats.Monad
import cats.instances.list._
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.functorFilter._
import scalax.collection.Graph
import scalax.collection.GraphEdge.DiEdge
import scalax.collection.GraphTraversal.AnyConnected

import mindmap.model.Entity
import mindmap.model.Note
import mindmap.model.Tag
import mindmap.model.graph.SingleTag
import mindmap.model.graph.SingleNote
import mindmap.model.graph.OverlappingTags
import mindmap.model.graph.GraphNode
import mindmap.model.graph.GraphWarningAlgebra
import mindmap.model.graph.GraphWarning

class RealGraphWarnings[F[+_]: Monad[*[_]]] extends GraphWarningAlgebra[F] {
  private def singleTags(graph: Graph[Entity, DiEdge]): F[List[SingleTag]] = {
    graph.nodes.toList
      .mapFilter(node => {
        for {
          singleTag <- GraphNode
            .tagNode(node)
            .filter(_.node.degree <= 1)
            .map(_.tag)
        } yield (SingleTag(singleTag))
      })
      .pure[F]
  }

  private def singleNotes(graph: Graph[Entity, DiEdge]): F[List[SingleNote]] = {
    graph.nodes.toList
      .mapFilter(node => {
        for {
          singleNote <- GraphNode
            .noteNode(node)
            .filter(_.node.degree == 0)
            .map(_.note)
        } yield (SingleNote(singleNote))
      })
      .pure[F]
  }

  private def overlappingTags(
    graph: Graph[Entity, DiEdge]
  ): F[Iterable[OverlappingTags]] = {
    graph.nodes
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
          if (
            connectedNotes
              .filter(note => !connectedTag.neighbors.contains(note))
              .isEmpty
          ) {
            val overlappedTag = connectedTag.toOuter.asInstanceOf[Tag]
            Some(
              OverlappingTags(tag, overlappedTag)
            )
          } else {
            None
          }
        })
      })
      .pure[F]
  }

  def warnings(graph: Graph[Entity, DiEdge]): F[Iterable[GraphWarning]] = {
    for {
      tagWarnings <- singleTags(graph)
      noteWarnings <- singleNotes(graph)
      tagOverlapWarnings <- overlappingTags(graph)
    } yield {
      tagWarnings ++ noteWarnings ++ tagOverlapWarnings
    }
  }
}
