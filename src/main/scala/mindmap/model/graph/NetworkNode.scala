package mindmap.model.graph

import scala.math.sqrt

import mindmap.model.Note
import mindmap.model.Tag

case class NetworkNode(
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

object NetworkNode {
  implicit class NetworkNodeOps(node: NetworkNode) {
    def hidden: NetworkNode = NetworkNode.hidden(node)
    def shown: NetworkNode = NetworkNode.shown(node)
    def toggle(show: Boolean): NetworkNode = NetworkNode.toggle(node, show)
  }

  def hidden(node: NetworkNode): NetworkNode =
    node.copy(physics = Some(false), hidden = Some(true))
  def shown(node: NetworkNode): NetworkNode =
    node.copy(physics = Some(true), hidden = Some(false))
  def toggle(node: NetworkNode, show: Boolean): NetworkNode =
    if (show) shown(node) else hidden(node)

  def noteNode(idx: Long, label: String, content: String): NetworkNode =
    NetworkNode(
      idx,
      label,
      group = Some("note"),
      title = Some(content)
    )

  def tagNode(idx: Long, label: String): NetworkNode = NetworkNode(
    idx,
    label,
    group = Some("tag")
  )

  def clusterNode(idx: Long, tag: Tag, clustered: Seq[Note]): NetworkNode = {
    val content = (tag.name +: clustered.map(note => note.title)).mkString(", ")
    NetworkNode(
      idx,
      tag.name,
      group = Some("cluster"),
      title = Some(content),
      mass = Some(sqrt(clustered.size + 1))
    )
  }
}
