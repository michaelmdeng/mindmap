package mindmap.model.graph

import mindmap.model.Node
import mindmap.model.Edge

trait GraphAlgebra[F[_]] {
  def graph(): F[
    (Iterable[Node], Iterable[Edge], Map[String, Long], Map[String, List[Long]])
  ]
}
