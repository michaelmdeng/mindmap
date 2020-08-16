package mindmap.model.graph

import mindmap.model.network.NetworkNode
import mindmap.model.network.NetworkEdge

trait GraphAlgebra[F[_]] {
  def graph(): F[
    (
      Iterable[NetworkNode],
      Iterable[NetworkEdge],
      Map[String, Long],
      Map[String, List[Long]]
    )
  ]
}
