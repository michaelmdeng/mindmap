package mindmap.model.graph

case class Network(
  nodes: Iterable[NetworkNode],
  edges: Iterable[NetworkEdge],
  clusterTags: Map[NetworkNode, NetworkNode],
  clusterNotes: Map[NetworkNode, Iterable[NetworkNode]]
)
