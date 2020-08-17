package mindmap.model.graph

import mindmap.model.Note
import mindmap.model.Tag

case class Cluster(tag: Tag, notes: Seq[Note])
