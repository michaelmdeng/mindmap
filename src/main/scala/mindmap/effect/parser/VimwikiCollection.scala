package mindmap.effect.parser

import cats.Parallel
import cats.data.Chain
import cats.effect.ContextShift
import cats.effect.Effect
import cats.implicits._
import java.io.File
import java.nio.file.FileVisitOption
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import org.apache.log4j.Logger
import scala.jdk.StreamConverters._

import mindmap.effect.configuration.IgnoreFile
import mindmap.effect.parser.FileNoteParser
import mindmap.model.Collection
import mindmap.model.Note

object VimwikiCollection {
  private val logger: Logger = Logger.getLogger(this.getClass())
  private val MAX_DEPTH: Int = 100
  private val ignoreFile: IgnoreFile = new IgnoreFile()

  private def getFiles[F[_]: Effect[?[_]]](dir: File): F[List[File]] =
    for {
      files <- Effect[F].delay {
        Files
          .find(
            dir.toPath(),
            MAX_DEPTH,
            (path, attr) => !ignoreFile.isIgnoreFile(path),
            FileVisitOption.FOLLOW_LINKS
          )
          .toScala(List)
          .filter(Files.isRegularFile(_))
          .map(_.toFile())
      }
    } yield (files)

  def apply[F[_]: ContextShift[?[_]]: Effect[?[_]]: Parallel[?[_]]](
    dir: File
  ): F[Collection] =
    for {
      files <- getFiles(dir)
      notes <- files
        .map(file => {
          new FileNoteParser(file)
            .parseNote()
            .map(List(_))
            .handleError(e => List())
        })
        .parFlatSequence
    } yield (Collection(Chain.fromSeq(notes)))
}
