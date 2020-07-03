package mindmap.model

import cats.data.Chain

import mindmap.model.Note

case class Collection(notes: Chain[Note])
