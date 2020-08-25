package mindmap.model.graph

import scalax.collection.Graph
import scalax.collection.GraphEdge.DiEdge

import mindmap.model.Entity
import mindmap.model.Note
import mindmap.model.Tag

sealed trait TagNode {
  val node: Graph[Entity, DiEdge]#NodeT
  val tag: Tag
}

sealed trait NoteNode {
  val node: Graph[Entity, DiEdge]#NodeT
  val note: Note
}

object GraphNode {
  def noteNode(node: Graph[Entity, DiEdge]#NodeT): Option[NoteNode] = {
    node.toOuter match {
      case note: Note => Some(NoteNodeImpl(node, note))
      case _ => None
    }
  }

  def tagNode(node: Graph[Entity, DiEdge]#NodeT): Option[TagNode] = {
    node.toOuter match {
      case tag: Tag => Some(TagNodeImpl(node, tag))
      case _ => None
    }
  }

  private case class NoteNodeImpl(
    val node: Graph[Entity, DiEdge]#NodeT,
    val note: Note
  ) extends NoteNode

  private case class TagNodeImpl(
    val node: Graph[Entity, DiEdge]#NodeT,
    val tag: Tag
  ) extends TagNode
}
