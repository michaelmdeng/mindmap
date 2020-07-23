package mindmap.effect.graph

import cats.Applicative
import cats.implicits._
import org.apache.log4j.Logger

import mindmap.model.Edge
import mindmap.model.Node
import mindmap.model.Note
import mindmap.model.Tag
import mindmap.model.Zettelkasten
import mindmap.model.graph.GraphAlgebra

class GraphGenerator[F[+_]: Applicative[?[_]]] extends GraphAlgebra[F] {
  private def logger = Logger.getLogger(this.getClass())

  def graph(zettelkasten: Zettelkasten): F[(Iterable[Node], Iterable[Edge])] = {
    val noteIndices =
      Map.from(zettelkasten.notes.zip(0 until zettelkasten.notes.size))

    val tagIndices = Map.from(
      zettelkasten.tags
        .zip((0 until zettelkasten.tags.size).map(_ + zettelkasten.notes.size))
    )

    val nodes = noteIndices.map {
      case (note, idx) => Node(idx.toLong, note.title, shape = Some("ellipse"))
    } ++ tagIndices.map {
      case (tag, idx) =>
        Node(idx.toLong, tag.name, shape = Some("box"), color = Some("red"))
    }

    zettelkasten.links
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
      .foreach {
        case (tag, _) =>
          logger.warn(f"Tag: ${tag.name} only has one linked note")
      }

    zettelkasten.notes
      .filter(note => {
        val noteLinks = zettelkasten.links
          .mapFilter(link => {
            (link.from, link.to) match {
              case (n1: Note, n2: Note) => Some(List(n1, n2))
              case (n: Note, _) => Some(List(n))
              case (_, n: Note) => Some(List(n))
              case (_, _) => None
            }
          })
          .flatten

        !noteLinks.contains(note)
      })
      .map(note => {
        logger
          .warn(f"Note: ${note.title} is not linked to any other notes or tags")
      })

    val allEdges = zettelkasten.links.map(link => {
      val fromIdx = link.from match {
        case (note: Note) => noteIndices(note)
        case (tag: Tag) => tagIndices(tag)
      }

      val toIdx = link.to match {
        case (note: Note) => noteIndices(note)
        case (tag: Tag) => tagIndices(tag)
      }

      Edge(fromIdx, toIdx, arrows = Some("to"))
    })

    // combine two edges in reverse directions into a single bi-directional edge
    val combinedEdges = allEdges
      .groupBy(edge =>
        (Math.min(edge.to, edge.from), Math.max(edge.to, edge.from))
      )
      .flatMap[Edge] {
        case (k, v) => {
          if (v.size >= 2) {
            Seq(Edge(k._1, k._2, Some("from,to")))
          } else {
            v
          }
        }
      }

    (nodes, combinedEdges).pure[F]
  }
}
