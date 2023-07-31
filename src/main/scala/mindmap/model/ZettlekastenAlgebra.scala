package mindmap.model

trait ZettelkastenAlgebra[F[_]] {
  def getNote(name: String): F[Option[Note]]

  def getTag(name: String): F[Option[Tag]]

  def getTagNotes(tag: Tag): F[Set[Note]]

  def notes(): F[Iterable[Note]]

  def tags(): F[Set[Tag]]

  def findNote(id: Long): F[Option[Note]]

  def findTag(id: Long): F[Option[Tag]]
}
