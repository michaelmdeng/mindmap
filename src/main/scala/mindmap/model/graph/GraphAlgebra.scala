package mindmap.model.graph

import mindmap.model.Node
import mindmap.model.Edge
import mindmap.model.Zettelkasten

trait GraphAlgebra[F[_]] {
  def graph(zettelkasten: Zettelkasten): F[(Iterable[Node], Iterable[Edge])]
}
