package mindmap.model

case class Edge(
  from: Long,
  to: Long,
  arrows: Option[String] = None
)
