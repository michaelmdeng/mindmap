package mindmap.model

import mindmap.model.Note
import mindmap.model.ResolvedLink
import mindmap.model.Tag

case class Zettelkasten(
  notes: List[Note],
  tags: Set[Tag],
  links: List[ResolvedLink]
) {
  val noteMap: Map[Long, Note] = notes.map(note => (note.id, note)).toMap
  val tagMap: Map[Long, Tag] = tags.map(tag => (tag.id, tag)).toMap

  def findNote(id: Long): Option[Note] = noteMap.get(id)
  def findTag(id: Long): Option[Tag] = tagMap.get(id)
}
