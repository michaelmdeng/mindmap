package mindmap.effect.graph

import cats.Applicative
import cats.implicits._
import org.apache.log4j.Logger

import mindmap.model.Edge
import mindmap.model.Edge.EdgeOps
import mindmap.model.Entity
import mindmap.model.Node
import mindmap.model.Node.NodeOps
import mindmap.model.Note
import mindmap.model.Tag
import mindmap.model.Zettelkasten
import mindmap.model.graph.GraphAlgebra

class GraphGenerator[F[+_]: Applicative[?[_]]] extends GraphAlgebra[F] {
  private def logger = Logger.getLogger(this.getClass())

  private val clusterThreshold: Int = 7
  private val clusterEnabled: Boolean = true

  def graph(zettelkasten: Zettelkasten): F[(Iterable[Node], Iterable[Edge])] = {
    val noteIdxs: Seq[Long] = (0 until zettelkasten.notes.size).map(_.toLong)
    val idxByNote: Map[Note, Long] =
      Map.from(zettelkasten.notes.zip(noteIdxs))
    val noteByIdx: Map[Long, Note] = idxByNote.map(_.swap)

    val tagIdxs: Seq[Long] = (0 until zettelkasten.tags.size)
      .map(_ + zettelkasten.notes.size)
      .map(_.toLong)
    val idxByTag: Map[Tag, Long] = Map.from(zettelkasten.tags.zip(tagIdxs))
    val tagByIdx: Map[Long, Tag] = idxByTag.map(_.swap)

    val entityEdges: List[Edge] = zettelkasten.links.mapFilter(link => {
      (link.from, link.to) match {
        case (n1: Note, n2: Note) => {
          for {
            from <- idxByNote.get(n1)
            to <- idxByNote.get(n2)
          } yield (Edge.tagEdge(from, to))
        }
        case (t: Tag, n: Note) => {
          for {
            from <- idxByTag.get(t)
            to <- idxByNote.get(n)
          } yield (Edge.tagEdge(from, to))
        }
        case _ => {
          logger.warn(
            f"Unexpected Zettelkasten link from : ${link.from} to ${link.to}"
          )
          None
        }
      }
    })

    val combinedEntityEdges = entityEdges
      .groupBy(edge => {
        (Math.min(edge.to, edge.from), Math.max(edge.to, edge.from))
      })
      .flatMap[Edge] {
        case ((from, to), v) => {
          if (v.size >= 2) {
            Seq(Edge.doubleEdge(from, to))
          } else {
            v
          }
        }
      }

    val edgesByTag: Map[Tag, List[Edge]] = Map.from(
      entityEdges
        .groupBy(_.from)
        .toList
        .mapFilter {
          case (fromIdx, edges) => {
            for {
              tag <- tagByIdx.get(fromIdx)
            } yield ((tag, edges))
          }
        }
    )

    val mostConnectedTagByNote: Map[Note, Tag] = edgesByTag
      .flatMap[(Note, (Tag, Int))] {
        case (tag, edges) => {
          edges.map(edge => (noteByIdx(edge.to), (tag, edges.size)))
        }
      }
      .groupMapReduce {
        case (note, _) => note
      } {
        case (_, (tag, numEdges)) => (tag, numEdges)
      } {
        case ((t1, n1), (t2, n2)) => if (n1 >= n2) (t1, n1) else (t2, n2)
      }
      .map {
        case (note, (tag, numEdges)) => (note, tag)
      }

    val notesByMostConnectedTag: Map[Tag, Seq[Note]] =
      mostConnectedTagByNote
        .map[(Tag, Note)](_.swap)
        .groupMap {
          case (tag, _) => tag
        } {
          case (_, note) => note
        }
        .map {
          case (tag, notes) => (tag, notes.toSeq)
        }

    val noteByClusteredTag: Map[Tag, Seq[Note]] =
      notesByMostConnectedTag.filter {
        case (_, notes) => notes.size > clusterThreshold
      }

    val clusterIdxs: Seq[Long] = (0 until noteByClusteredTag.keySet.size)
      .map(_ + zettelkasten.notes.size + zettelkasten.tags.size)
      .map(_.toLong)

    val clusterIdxByEntity: Map[Entity, Long] =
      Map.from(noteByClusteredTag.zip(clusterIdxs).flatMap {
        case ((tag, notes), clusterIdx) => {
          (tag, clusterIdx) +: notes.map((_, clusterIdx))
        }
      })

    val tagByClusterIdx: Map[Long, Tag] = Map.from(
      clusterIdxByEntity.toList
        .mapFilter {
          case (tag: Tag, idx) => Some((idx, tag))
          case _ => None
        }
    )

    val notesByClusterIdx: Map[Long, Seq[Note]] = clusterIdxByEntity
      .filter {
        case (note: Note, _) => true
        case _ => false
      }
      .groupMap {
        case (_, idx) => idx
      } {
        case (note, _) => note.asInstanceOf[Note]
      }
      .map {
        case (idx, notes) => (idx, notes.toSeq)
      }

    val allNodes = idxByNote.map {
      case (note, idx) => {
        val isClusterNote = clusterIdxByEntity.contains(note)
        Node
          .noteNode(idx, note.title, note.content)
          .toggle(!isClusterNote || !clusterEnabled)
      }
    } ++ idxByTag.map {
      case (tag, idx) => {
        val isClusterTag = clusterIdxByEntity.contains(tag)
        Node.tagNode(idx, tag.name).toggle(!isClusterTag || !clusterEnabled)
      }
    } ++ tagByClusterIdx.map {
      case (clusterIdx, tag) => {
        Node
          .clusterNode(
            clusterIdx,
            tagByClusterIdx(clusterIdx),
            notesByClusterIdx(clusterIdx)
          )
          .toggle(clusterEnabled)
      }
    }

    val allEdges: Iterable[Edge] = combinedEntityEdges
      .flatMap(edge => {
        if (tagByIdx.contains(edge.from) && clusterIdxByEntity
            .contains(tagByIdx(edge.from))) {
          List(
            edge.toggle(!clusterEnabled),
            edge
              .copy(from = clusterIdxByEntity(tagByIdx(edge.from)))
              .toggle(clusterEnabled)
          )
        } else if (noteByIdx.contains(edge.from) && clusterIdxByEntity
            .contains(noteByIdx(edge.from))) {
          List(
            edge.toggle(!clusterEnabled),
            edge
              .copy(from = clusterIdxByEntity(noteByIdx(edge.from)))
              .toggle(clusterEnabled)
          )
        } else if (noteByIdx.contains(edge.to) && clusterIdxByEntity
            .contains(noteByIdx(edge.to))) {
          List(
            edge.toggle(!clusterEnabled),
            edge
              .copy(to = clusterIdxByEntity(noteByIdx(edge.to)))
              .toggle(clusterEnabled)
          )
        } else {
          List(edge)
        }
      })
      .groupMapReduce(edge => {
        (edge.from, edge.to)
      })(edge => edge)((e1, e2) => e1)
      .values

    (allNodes, allEdges).pure[F]
  }
}
