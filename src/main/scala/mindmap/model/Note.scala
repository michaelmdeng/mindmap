package mindmap.model

import cats.Show
import cats.Eq
import cats.data.Chain
import java.time.LocalDateTime

case class Note(
  content: String,
  createDate: LocalDateTime,
  modifiedDate: LocalDateTime,
  title: String,
  id: Option[Long]
) extends Entity

object Note {
  def apply(
    content: String,
    createDate: LocalDateTime,
    modifiedDate: LocalDateTime,
    title: String
  ): Note =
    Note(
      content = content,
      createDate = createDate,
      modifiedDate = modifiedDate,
      title,
      id = None
    )

  implicit def showForNote: Show[Note] = new Show[Note] {
    def show(note: Note): String = {
      f"Note(<content>, ${note.createDate}, ${note.modifiedDate}, ${note.title}, ${note.id})"
    }
  }

  implicit def eqNote: Eq[Note] = new Eq[Note] {
    def eqv(x: Note, y: Note): Boolean = {
      x.title == y.title
    }
  }
}
