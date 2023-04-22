package mindmap.model.configuration

import mindmap.model.Tag

trait GraphConfigurationAlgebra[F[_]] {
  def clusteringEnabled(): F[Boolean]
  def clusterThreshold(): F[Int]
  def isIgnoreClusterTag(tag: Tag): F[Boolean]
}
