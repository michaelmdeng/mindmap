package mindmap.model.parser

import java.time.LocalDateTime

trait MetadataParserAlgebra[F[_]] {
  def createDate(): F[LocalDateTime]
  def title(): F[String]
  def modifiedDate(): F[LocalDateTime]
}
