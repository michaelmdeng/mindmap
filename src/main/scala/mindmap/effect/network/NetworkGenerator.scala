// package mindmap.effect.network

// import cats.Monad
// import cats.instances.list._
// import cats.syntax.flatMap._
// import cats.syntax.functor._
// import cats.syntax.functorFilter._
// import org.apache.log4j.Logger
// import scalax.collection.Graph
// import scalax.collection.GraphEdge.DiEdge

// import mindmap.model.Entity
// import mindmap.model.Note
// import mindmap.model.Tag
// import mindmap.model.configuration.ConfigurationAlgebra
// import mindmap.model.network.Cluster
// import mindmap.model.network.Network
// import mindmap.model.network.NetworkAlgebra
// import mindmap.model.network.NetworkEdge
// import mindmap.model.network.NetworkEdge.NetworkEdgeOps
// import mindmap.model.network.NetworkNode
// import mindmap.model.network.NetworkNode.NetworkNodeOps

// class NetworkGenerator[F[_]: Monad[?[_]]](config: ConfigurationAlgebra[F])
//     extends NetworkAlgebra[F] {
//   private def logger = Logger.getLogger(this.getClass())

//   private def clusters(graph: Graph[Entity, DiEdge]): F[Iterable[Cluster]] =
//     for {
//       graphConfig <- config.graphConfiguration
//     } yield {
//       val clusterByTagNode = graph.nodes.toList
//         .mapFilter(node => {
//           node.toOuter match {
//             case _: Tag => Some((node, node.neighbors.size))
//             case _ => None
//           }
//         })
//         .toMap

//       clusterByTagNode.toList.mapFilter {
//         case (tagNode, size) => {
//           val clusterNotes = tagNode.neighbors
//             .filter(neighborNode => {
//               neighborNode.toOuter match {
//                 case _: Note => {
//                   neighborNode.neighbors
//                     .filter(noteNeighborNode => {
//                       clusterByTagNode.contains(noteNeighborNode)
//                     })
//                     .maxByOption(neighborTagNode => {
//                       clusterByTagNode(neighborTagNode)
//                     }) match {
//                     case Some(t) => tagNode == t
//                     case _ => false
//                   }
//                 }
//                 case _ => false
//               }
//             })
//             .toList
//             .mapFilter(node => {
//               node.toOuter match {
//                 case note: Note => Some(note)
//                 case _ => None
//               }
//             })

//           if (clusterNotes.size >= graphConfig.clusterThreshold) {
//             tagNode.toOuter match {
//               case tag: Tag => Some(Cluster(tag, clusterNotes))
//               case _ => None
//             }
//           } else {
//             None
//           }
//         }
//       }
//     }

//   def network(graph: Graph[Entity, DiEdge]): F[Network] =
//     for {
//       graphConfig <- config.graphConfiguration
//       clusters <- clusters(graph)
//     } yield {
//       val idxByCluster = clusters.zipWithIndex.map {
//         case (cluster, idx) => (cluster, (idx + graph.nodes.size).toLong)
//       }.toMap

//       val clusterIdxByEntity: Map[Entity, Long] = idxByCluster.keySet
//         .flatMap(cluster => {
//           (cluster.tag +: cluster.notes)
//             .map(entity => (entity, idxByCluster(cluster)))
//         })
//         .toMap

//       val idxByEntity: Map[Entity, Long] = graph.nodes.zipWithIndex.map {
//         case (node, idx) => {
//           node.toOuter match {
//             case note: Note => (note, idx.toLong)
//             case tag: Tag => (tag, idx.toLong)
//           }
//         }
//       }.toMap

//       val clusterEnabled = graphConfig.clusterEnabled
//       val networkNodes = graph.nodes.zipWithIndex.map {
//         case (node, idx) => {
//           node.toOuter match {
//             case note: Note => {
//               val isClusterNote = clusterIdxByEntity.contains(note)
//               NetworkNode
//                 .noteNode(idx.toLong, note.title, note.content)
//                 .toggle(!isClusterNote || !clusterEnabled)
//             }
//             case tag: Tag => {
//               val isClusterTag = clusterIdxByEntity.contains(tag)
//               NetworkNode
//                 .tagNode(idx.toLong, tag.name)
//                 .toggle(!isClusterTag || !clusterEnabled)
//             }
//           }
//         }
//       } ++ idxByCluster.map {
//         case (cluster, idx) => {
//           NetworkNode.clusterNode(
//             idx,
//             cluster.tag,
//             cluster.notes
//           )
//         }
//       }
//       val nodeByIdx = networkNodes.map(node => (node.id, node)).toMap

//       val networkEdges = graph.edges
//         .groupBy(edge => {
//           (edge.from.toOuter, edge.to.toOuter) match {
//             case (n1: Note, n2: Note) => {
//               if (n1.title < n2.title) {
//                 (n1, n2)
//               } else {
//                 (n2, n1)
//               }
//             }
//             case (t: Tag, n: Note) => {
//               if (t.name < n.title) {
//                 (t, n)
//               } else {
//                 (n, t)
//               }
//             }
//             // case _ => {
//             //   logger.warn(
//             //     f"Unexpected link from ${edge.from.toOuter} to ${edge.to.toOuter}"
//             //   )
//             //   1
//             // }
//           }
//         })
//         .map {
//           case (_, v) => v.head
//         }
//         .flatMap(graphEdge => {
//           val edge = (graphEdge.from.toOuter, graphEdge.to.toOuter) match {
//             case (n1: Note, n2: Note) => {
//               NetworkEdge.noteEdge(idxByEntity(n1), idxByEntity(n2))
//             }
//             case (t: Tag, n: Note) => {
//               NetworkEdge.tagEdge(idxByEntity(t), idxByEntity(n))
//             }
//             // case _ => {
//             //   NetworkEdge.noteEdge(-1, -1)
//             // }
//           }

//           (
//             clusterIdxByEntity.get(graphEdge.from.toOuter),
//             clusterIdxByEntity.get(graphEdge.to.toOuter)
//           ) match {
//             case (Some(fromCluster), Some(toCluster))
//                 if fromCluster != toCluster => {
//               List(
//                 edge.toggle(!clusterEnabled),
//                 edge
//                   .copy(from = fromCluster, to = toCluster)
//                   .toggle(clusterEnabled),
//                 edge
//                   .copy(from = fromCluster)
//                   .toggle(!clusterEnabled),
//                 edge
//                   .copy(to = toCluster)
//                   .toggle(!clusterEnabled)
//               )
//             }
//             case (Some(commonCluster), Some(_)) => {
//               List(edge.toggle(!clusterEnabled))
//             }
//             case (Some(fromCluster), None) => {
//               List(
//                 edge.toggle(!clusterEnabled),
//                 edge.copy(from = fromCluster).toggle(clusterEnabled)
//               )
//             }
//             case (None, Some(toCluster)) => {
//               List(
//                 edge.toggle(!clusterEnabled),
//                 edge.copy(to = toCluster).toggle(clusterEnabled)
//               )
//             }
//             case (None, None) => List(edge)
//           }
//         })

//       val clusterTags = idxByCluster.keySet
//         .flatMap(cluster => {
//           val t = (
//             nodeByIdx(idxByCluster(cluster)),
//             nodeByIdx(idxByEntity(cluster.tag))
//           )
//           Set(t, t.swap)
//         })
//         .toMap

//       val clusterNotes = idxByCluster.keySet
//         .map(cluster => {
//           (
//             nodeByIdx(idxByCluster(cluster)),
//             cluster.notes.map(note => nodeByIdx(idxByEntity(note)))
//           )
//         })
//         .toMap

//       Network(
//         nodes = networkNodes,
//         edges = networkEdges,
//         clusterTags = clusterTags,
//         clusterNotes = clusterNotes
//       )
//     }
// }
