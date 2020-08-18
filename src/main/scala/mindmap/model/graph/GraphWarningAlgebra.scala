package mindmap.model.graph

import scalax.collection.Graph
import scalax.collection.GraphEdge.DiEdge

import mindmap.model.Entity

trait GraphWarningAlgebra[F[_]] {
  def warnings(graph: Graph[Entity, DiEdge]): F[Iterable[String]]
}
