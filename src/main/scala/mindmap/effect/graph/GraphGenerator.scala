package mindmap.effect.graph

import cats.Monad
import cats.instances.list._
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.functorFilter._
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

class GraphGenerator[F[+_]: Monad[*[_]]: ConfigurationAlgebra[*[_]]](
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

  private def cluster(graph: Graph[Entity, DiEdge]): F[
    (Map[Either[Entity, Cluster], Long], Map[Either[Entity, Cluster], Long])
  ] =
    for {
      graphConfig <- ConfigurationAlgebra[F].graphConfiguration
    } yield {
      val clusterByTagNode = graph.nodes.toList
        .mapFilter(node => GraphNode.tagNode(node))
        .filter(tagNode =>
          !graphConfig.excludeClusterTags.contains(tagNode.tag)
        )
        .map(tagNode => (tagNode, tagNode.node.neighbors.size))
        .toMap

      val clusters = clusterByTagNode.toList.mapFilter {
        case (tagNode, size) => {
          val clusterNotes = tagNode.node.neighbors.toList
            .mapFilter(neighborNode => {
              for {
                neighborNoteNode <- GraphNode.noteNode(neighborNode)
                isCluster <- neighborNoteNode.node.neighbors.toList
                  .mapFilter(noteNeighbor => GraphNode.tagNode(noteNeighbor))
                  .maxByOption(neighborTagNode => {
                    clusterByTagNode.get(neighborTagNode).getOrElse(-1)
                  })
                  .filter(_ == tagNode)
              } yield (neighborNoteNode.note)
            })

          if (clusterNotes.size >= graphConfig.clusterThreshold) {
            Some(Cluster(tagNode.tag, clusterNotes))
          } else {
            None
          }
        }
      }

      val idxByMember = (graph.nodes.zipWithIndex.map {
        case (node, idx) => (Left(node.toOuter), idx.toLong)
      } ++ clusters.zipWithIndex.map {
        case (cluster, idx) =>
          (Right(cluster), (idx + graph.nodes.size).toLong)
      }).toMap

      val clusterIdxByMember: Map[Either[Entity, Cluster], Long] = clusters
        .flatMap[(Either[Entity, Cluster], Long)](cluster => {
          (Seq(Right(cluster), Left(cluster.tag)) ++ cluster.notes.map(Left(_)))
            .map(member => (member, idxByMember(Right(cluster))))
        })
        .toMap

      (idxByMember, clusterIdxByMember)
    }

  def network(graph: Graph[Entity, DiEdge]): F[Network] =
    for {
      graphConfig <- ConfigurationAlgebra[F].graphConfiguration
      t <- cluster(graph)
      idxByMember = t._1
      clusterIdxByMember = t._2
    } yield {
      val clusterEnabled = graphConfig.clusterEnabled
      val networkNodes = idxByMember.map {
        case (Left(e), idx) => {
          val isCluster = clusterIdxByMember.contains(Left(e))
          e match {
            case note: Note => {
              NetworkNode
                .noteNode(idx, note.title, note.content)
                .toggle(!isCluster || !clusterEnabled)
            }
            case tag: Tag => {
              NetworkNode
                .tagNode(idx, tag.name)
                .toggle(!isCluster || !clusterEnabled)
            }
          }
        }
        case (Right(cluster), idx) => {
          NetworkNode
            .clusterNode(idx, cluster.tag, cluster.notes)
            .toggle(clusterEnabled)
        }
      }

      val networkEdges = graph.edges
        .groupBy(edge => {
          (edge.from.toOuter, edge.to.toOuter) match {
            case (e1, e2) => {
              (
                Math.min(idxByMember(Left(e1)), idxByMember(Left(e2))),
                Math.max(idxByMember(Left(e1)), idxByMember(Left(e2)))
              )
            }
          }
        })
        .map {
          case (_, v) => v.head
        }
        .flatMap(graphEdge => {
          val edge = (graphEdge.from.toOuter, graphEdge.to.toOuter) match {
            case (e1, e2) => {
              NetworkEdge.noteEdge(idxByMember(Left(e1)), idxByMember(Left(e2)))
            }
          }

          (
            clusterIdxByMember.get(Left(graphEdge.from.toOuter)),
            clusterIdxByMember.get(Left(graphEdge.to.toOuter))
          ) match {
            case (Some(fromCluster), Some(toCluster))
                if fromCluster != toCluster => {
              List(
                edge.toggle(!clusterEnabled),
                edge
                  .copy(from = fromCluster, to = toCluster)
                  .toggle(clusterEnabled),
                edge.copy(from = fromCluster).toggle(!clusterEnabled),
                edge.copy(to = toCluster).toggle(!clusterEnabled)
              )
            }
            case (Some(commonCluster), Some(_)) => {
              List(edge.toggle(!clusterEnabled))
            }
            case (Some(fromCluster), None) => {
              List(
                edge.toggle(!clusterEnabled),
                edge.copy(from = fromCluster).toggle(clusterEnabled)
              )
            }
            case (None, Some(toCluster)) => {
              List(
                edge.toggle(!clusterEnabled),
                edge.copy(to = toCluster).toggle(clusterEnabled)
              )
            }
            case (None, None) => List(edge)
          }
        })

      val nodeByIdx = networkNodes.map(node => (node.id, node)).toMap

      Network(
        nodes = networkNodes,
        edges = networkEdges
      )
    }
}
