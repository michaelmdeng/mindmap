package mindmap.effect.parser

import cats.effect.ContextShift
import cats.effect.Effect
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant
import java.time.LocalDateTime
import java.util.TimeZone
import org.apache.commons.io.FilenameUtils
import org.apache.logging.log4j.Level
import scala.concurrent.ExecutionContext
import scala.io.Source
import tofu.logging.Logging
import tofu.syntax.logging._

import mindmap.model.Note
import mindmap.model.parser.NoteParserAlgebra

class FileNoteParser[F[_]: ContextShift[*[_]]: Effect[*[_]]: Logging.Make](
  file: File
) extends NoteParserAlgebra[F] {
  private implicit val log: Logging[F] =
    Logging.Make[F].forService[FileNoteParser[F]]

  def content(): F[String] =
    for {
      lines <- debug"read file: ${file.toString()}" >>
        Effect[F]
          .delay(Source.fromFile(file).getLines()) <* ContextShift[F].shift
      c <- debug"generate file content: ${file.toString()}" >>
        Effect[F].delay {
          lines.reduceOption(_ + "\n" + _).getOrElse("")
        } <* ContextShift[F].shift
    } yield (c)

  def createDate(): F[LocalDateTime] = Effect[F].delay {
    LocalDateTime.ofInstant(
      Instant.ofEpochMilli(
        Files
          .readAttributes(file.toPath(), classOf[BasicFileAttributes])
          .creationTime()
          .toMillis()
      ),
      TimeZone.getDefault().toZoneId()
    )
  }

  def modifiedDate(): F[LocalDateTime] = Effect[F].delay {
    LocalDateTime.ofInstant(
      Instant.ofEpochMilli(file.lastModified()),
      TimeZone.getDefault().toZoneId()
    )
  }

  def title(): F[String] = {
    Effect[F].delay(FilenameUtils.getBaseName(file.getName()))
  }

  def parseNote(): F[Note] =
    for {
      - <- info"parse note: ${file.toString()}"
      c <- content() <* ContextShift[F].shift
      cd <- createDate()
      md <- modifiedDate()
      i <- title()
    } yield {
      Note(
        content = c,
        createdDate = cd,
        modifiedDate = md,
        title = i,
        path = file.toPath()
      )
    }
}
