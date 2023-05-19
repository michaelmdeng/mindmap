package mindmap.effect

import cats.MonadError
import cats.syntax.applicative._
import cats.syntax.functor._
import scalax.collection.Graph
import scalax.collection.GraphEdge.DiEdge

import mindmap.model.Entity
import mindmap.model.MindmapAlgebra
import mindmap.model.Note
import mindmap.model.Tag
import mindmap.model.graph.Network
import mindmap.model.graph.NetworkNode
import mindmap.model.graph.NetworkEdge
import java.nio.file.NotLinkException

class MemoryMindmap[F[_]: MonadError[*[_], Throwable]](
  graph: Graph[Entity, DiEdge],
  network: Network
) extends MindmapAlgebra[F] {
  def network(): F[Network] = network.pure[F]

  def subnetwork(entity: Entity): F[Network] = for {
    node <- MonadError[F, Throwable].fromOption(
      graph.find(entity),
      new Exception("No entity")
    )
    idxByNode = Set(node)
      .union(node.neighbors)
      .map(_.toOuter)
      .zipWithIndex
      .toMap
    nodes = idxByNode.map { case (entity, idx) =>
      entity match {
        case note: Note => NetworkNode.noteNode(idx, note.title, note.content)
        case tag: Tag => NetworkNode.tagNode(idx, tag.name)
      }
    }
    edges = node.edges.map { edge =>
      val source = idxByNode(edge.source.toOuter)
      val target = idxByNode(edge.target.toOuter)
      NetworkEdge.noteEdge(source, target)
    }
  } yield (Network(nodes = nodes, edges = edges))
}
