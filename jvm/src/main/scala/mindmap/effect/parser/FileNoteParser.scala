package mindmap.effect.parser

import cats.effect.Effect
import cats.implicits._
import java.io.File
import java.nio.file.{Files, LinkOption}
import java.nio.file.attribute.BasicFileAttributes
import java.time.{Instant, LocalDateTime}
import java.util.TimeZone
import org.apache.commons.io.FilenameUtils
import scala.io.Source

import mindmap.model.parser._

class FileNoteParser[F[_]: Effect[?[_]]](file: File)
    extends ContentParserAlgebra[F]
    with MetadataParserAlgebra[F] {
  def content(): F[String] =
    for {
      c <- Effect[F].delay {
        Source
          .fromFile(file)
          .getLines()
          .reduce(_ + "\n" + _)
      }
    } yield (c)

  def createDate(): F[LocalDateTime] = Effect[F].delay {
    val path = file.toPath()

    LocalDateTime.ofInstant(
      Instant.ofEpochMilli(
        Files
          .readAttributes(path, classOf[BasicFileAttributes])
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

  def id(): F[String] = {
    Effect[F].delay(FilenameUtils.getBaseName(file.getName()))
  }
}
