package mindmap.model

import scala.math.sqrt

case class Node(
  id: Long,
  label: String,
  shape: Option[String] = None,
  color: Option[String] = None,
  title: Option[String] = None,
  group: Option[String] = None,
  physics: Option[Boolean] = None,
  hidden: Option[Boolean] = None,
  mass: Option[Double] = None
)

object Node {
  implicit class NodeOps(node: Node) {
    def hidden: Node = Node.hidden(node)
    def shown: Node = Node.shown(node)
    def toggle(show: Boolean): Node = Node.toggle(node, show)
  }

  def hidden(node: Node): Node =
    node.copy(physics = Some(false), hidden = Some(true))
  def shown(node: Node): Node =
    node.copy(physics = Some(true), hidden = Some(false))
  def toggle(node: Node, show: Boolean): Node =
    if (show) shown(node) else hidden(node)

  def noteNode(idx: Long, label: String, content: String): Node = Node(
    idx,
    label,
    shape = Some("ellipse"),
    color = Some("lightblue"),
    title = Some(content)
  )

  def tagNode(idx: Long, label: String): Node = Node(
    idx,
    label,
    shape = Some("box"),
    color = Some("lightcoral")
  )

  def clusterNode(idx: Long, tag: Tag, clustered: Seq[Note]): Node = {
    val content = (tag.name +: clustered.map(note => note.title)).mkString(", ")
    Node(
      idx,
      f"Cluster: ${tag.name}",
      shape = Some("ellipse"),
      color = Some("moccasin"),
      title = Some(content),
      mass = Some(sqrt(clustered.size + 1))
    )
  }
}
