package mindmap.effect.graph

import cats.Applicative
import cats.syntax.applicative._
import scalax.collection.Graph
import scalax.collection.GraphEdge.DiEdge

import mindmap.model.Entity
import mindmap.model.Note
import mindmap.model.Tag
import mindmap.model.Zettelkasten
import mindmap.model.configuration.ConfigurationAlgebra
import mindmap.model.graph.Cluster
import mindmap.model.graph.GraphAlgebra
import mindmap.model.graph.GraphNode
import mindmap.model.graph.Network
import mindmap.model.graph.NetworkEdge
import mindmap.model.graph.NetworkEdge.NetworkEdgeOps
import mindmap.model.graph.NetworkNode
import mindmap.model.graph.NetworkNode.NetworkNodeOps

class GraphGenerator[F[+_]: Applicative[*[_]]: ConfigurationAlgebra[
  *[_]
]](
  zettelkasten: Zettelkasten
) extends GraphAlgebra[F] {
  def graph(): F[Graph[Entity, DiEdge]] = {
    Graph
      .from(
        zettelkasten.notes ++ zettelkasten.tags,
        zettelkasten.links.map(link => DiEdge(link.from, link.to))
      )
      .pure[F]
  }

  def network(graph: Graph[Entity, DiEdge]): F[Network] = {
    val idxByNode = graph.nodes.zipWithIndex.map { case (node, idx) =>
      (node.toOuter, idx.toLong)
    }.toMap

    val networkNodes = graph.nodes.map(node => {
      node.toOuter match {
        case note: Note =>
          NetworkNode.noteNode(idxByNode(note), note.title, note.content)
        case tag: Tag => NetworkNode.tagNode(idxByNode(tag), tag.name)
      }
    })

    val networkEdges = graph.edges
      // De-dupe edges
      .groupBy(edge => {
        val source = idxByNode(edge.source.toOuter)
        val target = idxByNode(edge.target.toOuter)
        (Math.min(source, target), Math.max(source, target))
      })
      .map { case ((source, target), _) =>
        NetworkEdge(source, target)
      }

    Network(nodes = networkNodes, edges = networkEdges).pure[F]
  }
}
