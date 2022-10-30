package mindmap.model.graph

case class NetworkEdge(
  from: Long,
  to: Long,
  arrows: Option[String] = None,
  smooth: Option[NetworkEdge.Smooth] = Some(NetworkEdge.Smooth("continuous")),
  dashes: Option[Boolean] = None,
  width: Option[Int] = None,
  color: Option[String] = None,
  physics: Option[Boolean] = None,
  hidden: Option[Boolean] = None
)

object NetworkEdge {
  case class Smooth(`type`: String)

  implicit class NetworkEdgeOps(edge: NetworkEdge) {
    def hide: NetworkEdge = NetworkEdge.hide(edge)
    def show: NetworkEdge = NetworkEdge.show(edge)
    def toggle(show: Boolean): NetworkEdge = NetworkEdge.toggle(edge, show)
  }

  def hide(edge: NetworkEdge): NetworkEdge =
    edge.copy(physics = Some(false), hidden = Some(true))
  def show(edge: NetworkEdge): NetworkEdge =
    edge.copy(physics = Some(true), hidden = Some(false))
  def toggle(edge: NetworkEdge, shouldShow: Boolean): NetworkEdge =
    if (shouldShow) show(edge) else hide(edge)

  def tagEdge(from: Long, to: Long): NetworkEdge = NetworkEdge(from, to)
  def noteEdge(from: Long, to: Long): NetworkEdge =
    NetworkEdge(from, to, arrows = Some("to"))
  def doubleEdge(from: Long, to: Long): NetworkEdge =
    NetworkEdge(from, to, arrows = Some("from,to"))
}
