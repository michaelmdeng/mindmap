package mindmap.model.parser

import mindmap.model.Tag

trait TagParserAlgebra[F[_]] {
  def tags(): F[Set[Tag]]
}
