package mindmap.model.graph

import cats.data.Chain

import mindmap.model.Node
import mindmap.model.Edge
import mindmap.model.Zettelkasten

trait GraphAlgebra[F[_]] {
  def graph(zettelkasten: Zettelkasten): F[(Chain[Node], Chain[Edge])]
}
