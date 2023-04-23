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
import org.apache.logging.log4j.Level
import scala.jdk.StreamConverters._
import tofu.logging.Logging
import tofu.syntax.logging._

import mindmap.effect.parser.FileNoteParser
import mindmap.model.Collection
import mindmap.model.configuration.ConfigurationAlgebra
import mindmap.model.parser.CollectionParserAlgebra

class VimwikiCollectionParser[F[_]: ContextShift[*[_]]: Effect[*[_]]: Parallel[
  *[_]
]: ConfigurationAlgebra[*[_]]: Logging.Make]
    extends CollectionParserAlgebra[F] {
  private implicit val log: Logging[F] =
    Logging.Make[F].forService[FileNoteParser[F]]

  private def useFiles[A](f: File => F[A]): F[LazyList[A]] = {
    for {
      root <- ConfigurationAlgebra[F].root()
      maxDepth <- ConfigurationAlgebra[F].maxDepth()
      out <- Resource
        .fromAutoCloseable(Effect[F].delay {
          Files
            .find(
              root.toPath(),
              maxDepth,
              (path, attr) => {
                !Effect[F]
                  .toIO(ConfigurationAlgebra[F].isIgnoreFile(path).attempt)
                  .unsafeRunSync()
                  .getOrElse(false)
              },
              FileVisitOption.FOLLOW_LINKS
            )
            .filter(Files.isRegularFile(_))
            .map(_.toFile())
        })
        .use(fileStream => fileStream.map(f(_)).toScala(LazyList).parSequence)
    } yield (out)
  }

  def parseCollection(config: ConfigurationAlgebra[F]): F[Collection] =
    for {
      notes <- info"traverse files" >> useFiles(file => {
        new FileNoteParser(file).parseNote()
      })
    } yield (Collection(notes.toList))
}
