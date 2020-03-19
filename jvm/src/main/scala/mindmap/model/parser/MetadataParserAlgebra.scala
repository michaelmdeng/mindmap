package mindmap.model.parser

import java.time.LocalDateTime

trait MetadataParserAlgebra[F[_]] {
  def createDate(): F[LocalDateTime]
  def id(): F[String]
  def modifiedDate(): F[LocalDateTime]
}
