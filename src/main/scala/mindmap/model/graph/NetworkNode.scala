package mindmap.model.graph

import scala.math.abs
import scala.math.sqrt
import scala.util.Random

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
  x: Option[Long] = None,
  y: Option[Long] = None
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

  def noteNode(label: String, content: String): NetworkNode =
    NetworkNode(
      abs(Random.nextLong()),
      label,
      group = Some("note")
    )

  def tagNode(label: String): NetworkNode = NetworkNode(
    abs(Random.nextLong()),
    label,
    group = Some("tag")
  )
}
