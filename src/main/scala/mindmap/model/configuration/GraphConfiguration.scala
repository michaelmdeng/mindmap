package mindmap.model.configuration

import mindmap.model.Tag

case class GraphConfiguration(
  clusterEnabled: Boolean,
  clusterThreshold: Int,
  excludeClusterTags: Set[Tag]
)

object GraphConfiguration {
  val DEFAULT: GraphConfiguration =
    GraphConfiguration(
      clusterEnabled = false,
      clusterThreshold = 7,
      excludeClusterTags = Set()
    )
}
