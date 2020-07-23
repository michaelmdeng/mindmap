package mindmap.model

import mindmap.model.Note
import mindmap.model.ResolvedLink
import mindmap.model.Tag

case class Zettelkasten(
  notes: List[Note],
  tags: Set[Tag],
  links: List[ResolvedLink]
)
