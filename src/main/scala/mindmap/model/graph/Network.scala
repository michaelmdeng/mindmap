package mindmap.model.graph

case class Network(
  nodes: Iterable[NetworkNode],
  edges: Iterable[NetworkEdge]
)
