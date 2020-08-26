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

import mindmap.effect.Logging
import mindmap.model.Note
import mindmap.model.parser.NoteParserAlgebra

class FileNoteParser[F[_]: ContextShift[?[_]]: Effect[?[_]]](file: File)
    extends NoteParserAlgebra[F] {
  private val logger: Logging[F] = new Logging(this.getClass())

  def content(): F[String] =
    for {
      lines <- logger.action(f"read file: ${file}")(
        Effect[F].delay(Source.fromFile(file).getLines())
      )
      c <- ContextShift[F].shift *> logger.action(
        f"generate content for ${file}"
      )(Effect[F].delay(lines.reduce(_ + "\n" + _)))
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
    logger.action(f"parse note: ${file}")(for {
      c <- content()
      cd <- ContextShift[F].shift *> createDate()
      md <- ContextShift[F].shift *> modifiedDate()
      i <- ContextShift[F].shift *> title()
    } yield {
      Note(
        content = c,
        createDate = cd,
        modifiedDate = md,
        title = i
      )
    })
}
