package mindmap.model

case class Edge(
  from: Long,
  to: Long,
  arrows: Option[String] = None,
  smooth: Option[Edge.Smooth] = Some(Edge.Smooth("continuous")),
  dashes: Option[Boolean] = None,
  width: Option[Int] = None,
  color: Option[String] = Some("black"),
  physics: Option[Boolean] = None,
  hidden: Option[Boolean] = None
)

object Edge {
  case class Smooth(`type`: String)

  implicit class EdgeOps(edge: Edge) {
    def hidden: Edge = Edge.hidden(edge)
    def shown: Edge = Edge.shown(edge)
    def toggle(show: Boolean): Edge = Edge.toggle(edge, show)
  }

  def hidden(edge: Edge): Edge =
    edge.copy(physics = Some(false), hidden = Some(true))
  def shown(edge: Edge): Edge =
    edge.copy(physics = Some(true), hidden = Some(false))
  def toggle(edge: Edge, show: Boolean): Edge =
    if (show) shown(edge) else hidden(edge)

  def tagEdge(from: Long, to: Long): Edge = Edge(from, to)
  def noteEdge(from: Long, to: Long): Edge = Edge(from, to, arrows = Some("to"))
  def doubleEdge(from: Long, to: Long): Edge =
    Edge(from, to, arrows = Some("from,to"))
}
