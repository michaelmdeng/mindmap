package mindmap.model.configuration

case class GraphConfiguration(clusterEnabled: Boolean, clusterThreshold: Int)

object GraphConfiguration {
  val DEFAULT: GraphConfiguration =
    GraphConfiguration(clusterEnabled = true, clusterThreshold = 7)
}
