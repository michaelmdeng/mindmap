package mindmap.model.parser

import cats.data.Chain

import mindmap.model.Link

trait LinkParserAlgebra[F[_]] {
  def links(): F[Chain[Link]]
}
