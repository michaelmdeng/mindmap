package mindmap.model

case class Node(
  id: Long,
  label: String,
  shape: Option[String] = None,
  color: Option[String] = None
)
