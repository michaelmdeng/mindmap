package mindmap.model.configuration

import mindmap.model.Tag

case class NetworkConfiguration(
  clusterThreshold: Int,
  excludeClusterTags: Set[Tag]
)

object NetworkConfiguration {
  val DEFAULT: NetworkConfiguration =
    NetworkConfiguration(
      clusterThreshold = 12,
      excludeClusterTags = Set()
    )
}

trait NetworkConfigurationAlgebra[F[_]] {
  def clusterThreshold(): F[Int]
  def isIgnoreClusterTag(tag: Tag): F[Boolean]

  def networkConfig(): F[NetworkConfiguration]
}
