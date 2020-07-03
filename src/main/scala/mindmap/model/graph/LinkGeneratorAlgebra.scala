package mindmap.model.graph

import cats.data.Chain

import mindmap.model.Collection
import mindmap.model.ResolvedLink

trait LinkGeneratorAlgebra[F[_]] {
  def links(collection: Collection): F[Chain[ResolvedLink]]
}
