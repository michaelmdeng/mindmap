package mindmap.model

import cats.data.Chain
import java.time.LocalDateTime

case class Note(
  content: String,
  createDate: LocalDateTime,
  id: String,
  modifiedDate: LocalDateTime,
  links: Chain[Link],
  tags: Set[Tag]
)
