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

class MemoryMindmap[F[_]: MonadError[*[_], Throwable]](
  graph: Graph[Entity, DiEdge],
  network: Network
) extends MindmapAlgebra[F] {
  def network(): F[Network] = network.pure[F]

  def find(id: Long): F[Option[NetworkNode]] = {
    network.find(id).pure[F]
  }

  def subnetwork(entity: Entity): F[Network] = for {
    node <- MonadError[F, Throwable].fromOption(
      graph.find(entity),
      new Exception("No entity")
    )
    neighborhood = Set(node).union(node.neighbors)
    nodeMap = neighborhood
      .map(graphNode => {
        val entity = graphNode.toOuter
        val networkNode = entity match {
          case note: Note => NetworkNode.noteNode(note.title, note.content)
          case tag: Tag => NetworkNode.tagNode(tag.name)
        }
        (entity, networkNode)
      })
      .toMap
    edges = neighborhood
      .flatMap(_.edges)
      .filter(edge => {
        nodeMap.get(edge.source).isDefined &&
        nodeMap.get(edge.target).isDefined
      })
      // De-dupe edges
      .groupBy(edge => {
        val source = nodeMap(edge.source).id
        val target = nodeMap(edge.target).id
        (Math.min(source, target), Math.max(source, target))
      })
      .map { case ((source, target), edges) =>
        if (edges.size == 1)
          NetworkEdge.noteEdge(source, target)
        else {
          NetworkEdge(source, target, arrows = Some("to,from"))
        }
      }
  } yield (Network(nodes = nodeMap.values, edges = edges))
}
