package mindmap.model

import cats.data.Chain

import mindmap.model.Note
import mindmap.model.ResolvedLink
import mindmap.model.Tag

case class Zettelkasten(
  notes: Chain[Note],
  tags: Chain[Tag],
  links: Chain[ResolvedLink]
)
