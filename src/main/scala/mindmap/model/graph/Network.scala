package mindmap.model.graph

case class Network(
  nodes: Iterable[NetworkNode],
  edges: Iterable[NetworkEdge]
) {
  val nodeMap: Map[Long, NetworkNode] = nodes.map(node => (node.id, node)).toMap

  def find(id: Long): Option[NetworkNode] = nodeMap.get(id)
}
