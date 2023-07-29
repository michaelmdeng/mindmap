package mindmap.model

import mindmap.model.graph.Network
import mindmap.model.graph.NetworkNode

trait MindmapAlgebra[F[_]] {
  def network(): F[Network]

  def subnetwork(entity: Entity): F[Network]

  def find(id: Long): F[Option[NetworkNode]]
}
