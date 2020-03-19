package mindmap.model

import cats.data.Chain

trait IndexAlgebra[F[_]] {
  def notes(): F[Chain[Note]]
  def tags(): F[Set[Tag]]

  def findNoteByName(name: String): F[Note]

  def findNotesByTag(tag: Tag): F[Chain[Note]]

  def getLinkedNotes(note: Note): F[Chain[Note]]

  def getLinkingNotes(note: Note): F[Chain[Note]]
}
