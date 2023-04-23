package mindmap.model

import cats.Show
import cats.Eq
import java.time.LocalDateTime
import java.nio.file.Path

case class Note(
  content: String,
  createdDate: LocalDateTime,
  modifiedDate: LocalDateTime,
  title: String,
  path: java.nio.file.Path,
  id: Option[Long]
) extends Entity

object Note {
  def apply(
    content: String,
    createdDate: LocalDateTime,
    modifiedDate: LocalDateTime,
    title: String,
    path: java.nio.file.Path
  ): Note =
    Note(
      content = content,
      createdDate = createdDate,
      modifiedDate = modifiedDate,
      title = title,
      path = path,
      id = None
    )

  implicit def showForNote: Show[Note] = new Show[Note] {
    def show(note: Note): String = {
      f"Note(<content>, ${note.createdDate}, ${note.modifiedDate}, ${note.title}, ${note.path}, ${note.id})"
    }
  }

  implicit def eqNote: Eq[Note] = new Eq[Note] {
    def eqv(x: Note, y: Note): Boolean = {
      x.path == y.path
    }
  }
}
