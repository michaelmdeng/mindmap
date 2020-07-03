package mindmap.model

case class Tag(
  id: Option[Long],
  name: String
) extends Entity

object Tag {
  def apply(name: String): Tag = Tag(None, name)
}
