package mindmap.effect.graph

import cats.Monad
import cats.instances.list._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.functorFilter._
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
import mindmap.model.configuration.ConfigurationAlgebra

class GraphGenerator[F[+_]: Monad[?[_]]](
  zettelkasten: Zettelkasten,
  config: ConfigurationAlgebra[F]
) extends GraphAlgebra[F] {
  private def logger = Logger.getLogger(this.getClass())

  private val noteIdxs: Seq[Long] =
    (0 until zettelkasten.notes.size).map(_.toLong)
  private val idxByNote: Map[Note, Long] =
    Map.from(zettelkasten.notes.zip(noteIdxs))
  private val noteByIdx: Map[Long, Note] = idxByNote.map(_.swap)

  private val tagIdxs: Seq[Long] = (0 until zettelkasten.tags.size)
    .map(_ + zettelkasten.notes.size)
    .map(_.toLong)
  private val idxByTag: Map[Tag, Long] =
    Map.from(zettelkasten.tags.zip(tagIdxs))
  private val tagByIdx: Map[Long, Tag] = idxByTag.map(_.swap)

  private def entityEdges: List[Edge] = {
    zettelkasten.links.mapFilter(link => {
      (link.from, link.to) match {
        case (n1: Note, n2: Note) => {
          for {
            from <- idxByNote.get(n1)
            to <- idxByNote.get(n2)
          } yield (Edge.noteEdge(from, to))
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
  }

  private def clusterNotesByTag(): F[Map[Tag, Seq[Note]]] =
    for {
      graphConfig <- config.graphConfiguration
    } yield {
      val edgesByTag = Map.from(
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

      val mostConnectedTagByNote = edgesByTag
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

      val notesByMostConnectedTag = mostConnectedTagByNote
        .map[(Tag, Note)](_.swap)
        .groupMap {
          case (tag, _) => tag
        } {
          case (_, note) => note
        }
        .map {
          case (tag, notes) => (tag, notes.toSeq)
        }

      notesByMostConnectedTag.filter {
        case (_, notes) => notes.size > graphConfig.clusterThreshold
      }
    }

  def graph: F[
    (Iterable[Node], Iterable[Edge], Map[String, Long], Map[String, List[Long]])
  ] =
    for {
      graphConfig <- config.graphConfiguration
      noteByClusteredTag <- clusterNotesByTag()
    } yield {
      val clusterEnabled = graphConfig.clusterEnabled

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

      val allEdges = combinedEntityEdges
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

      val clusterTags = tagByClusterIdx.map {
        case (clusterIdx, tag) => (clusterIdx.toString(), idxByTag(tag))
      } ++ tagByClusterIdx.map {
        case (clusterIdx, tag) => (idxByTag(tag).toString(), clusterIdx)
      }

      val clusterNotes = notesByClusterIdx.map {
        case (clusterIdx, notes) =>
          (clusterIdx.toString, notes.map(idxByNote(_)).toList)
      }

      (allNodes, allEdges, clusterTags, clusterNotes)
    }
}
