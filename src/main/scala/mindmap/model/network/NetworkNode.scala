package mindmap.model.network

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
      shape = Some("ellipse"),
      color = Some("lightblue"),
      title = Some(content)
    )

  def tagNode(idx: Long, label: String): NetworkNode = NetworkNode(
    idx,
    label,
    shape = Some("box"),
    color = Some("lightcoral")
  )

  def clusterNode(idx: Long, tag: Tag, clustered: Seq[Note]): NetworkNode = {
    val content = (tag.name +: clustered.map(note => note.title)).mkString(", ")
    NetworkNode(
      idx,
      f"Cluster: ${tag.name}",
      shape = Some("ellipse"),
      color = Some("moccasin"),
      title = Some(content),
      mass = Some(sqrt(clustered.size + 1))
    )
  }
}
