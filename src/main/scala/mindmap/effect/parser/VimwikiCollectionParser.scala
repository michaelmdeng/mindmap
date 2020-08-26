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
import scala.jdk.StreamConverters._

import mindmap.effect.parser.FileNoteParser
import mindmap.model.Collection
import mindmap.model.configuration.ConfigurationAlgebra
import mindmap.model.parser.CollectionParserAlgebra

class VimwikiCollectionParser[F[_]: ContextShift[?[_]]: Effect[?[_]]: Parallel[
  ?[_]
]] extends CollectionParserAlgebra[F] {
  private val MAX_DEPTH: Int = 100

  private def getFiles(config: ConfigurationAlgebra[F]): F[List[File]] = {
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

  def parseCollection(config: ConfigurationAlgebra[F]): F[Collection] =
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
