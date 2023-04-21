package mindmap.model

trait ZettelkastenAlgebra[F[_]] {
  def getNote(name: String): F[Option[Note]]

  def getTag(name: String): F[Option[Tag]]

  def getTagNotes(tag: Tag): F[Set[Note]]
}
