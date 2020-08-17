package mindmap.model.graph

import scalax.collection.Graph
import scalax.collection.GraphEdge.DiEdge

import mindmap.model.Entity

trait GraphAlgebra[F[_]] {
  def graph(): F[Graph[Entity, DiEdge]]
  def network(graph: Graph[Entity, DiEdge]): F[Network]
}
