package mindmap.effect.parser

import cats.Parallel
import cats.effect.ContextShift
import cats.effect.Effect
import cats.effect.Resource
import cats.instances.list._
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.parallel._
import java.io.File
import java.nio.file.FileVisitOption
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import org.apache.log4j.Logger
import scala.jdk.StreamConverters._

import mindmap.model.configuration.ConfigurationAlgebra
import mindmap.effect.parser.FileNoteParser
import mindmap.model.Collection
import mindmap.model.Note

object VimwikiCollection {
  private val logger: Logger = Logger.getLogger(this.getClass())
  private val MAX_DEPTH: Int = 100
  private def getFiles[F[_]: Effect[?[_]]](
    config: ConfigurationAlgebra[F]
  ): F[List[File]] = {
    for {
      dir <- config.rootDir
      files <- Resource
        .fromAutoCloseable(Effect[F].delay {
          Files
            .find(
              dir.toPath(),
              MAX_DEPTH,
              (path, attr) => {
                !Effect[F]
                  .toIO(config.isIgnoreFile(path).attempt)
                  .unsafeRunSync()
                  .getOrElse(false)
              },
              FileVisitOption.FOLLOW_LINKS
            )
            .filter(Files.isRegularFile(_))
            .map(_.toFile())
        })
        .use(fileStream => fileStream.toScala(List).pure[F])
    } yield (files)
  }

  def apply[F[_]: ContextShift[?[_]]: Effect[?[_]]: Parallel[?[_]]](
    config: ConfigurationAlgebra[F]
  ): F[Collection] =
    for {
      files <- getFiles(config)
      notes <- files
        .map(file => {
          new FileNoteParser(file)
            .parseNote()
            .map(List(_))
            .handleError(e => List())
        })
        .parFlatSequence
    } yield (Collection(notes))
}
