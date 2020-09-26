package mindmap.effect.configuration

import cats.effect.Effect
import cats.syntax.applicative._
import cats.syntax.functor._
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.PathMatcher
import scala.io.Source

import mindmap.effect.Logging
import mindmap.model.configuration.CollectionConfiguration
import mindmap.model.configuration.ConfigurationAlgebra
import mindmap.model.configuration.GraphConfiguration
import mindmap.model.configuration.RepositoryConfiguration

object RealConfiguration {
  private val IGNORE_FILE = ".mindmapignore"
  private val DEFAULT_FS = FileSystems.getDefault()

  def apply[F[_]: Effect[?[_]]](root: String): ConfigurationAlgebra[F] =
    new ConfigurationAlgebra[F] {
      private val logger: Logging[F] = new Logging(this.getClass())
      private val markdownFiles: PathMatcher =
        DEFAULT_FS.getPathMatcher(f"glob:${root}/**.md")

      def isIgnoreFile(path: Path): F[Boolean] =
        for {
          config <- collectionConfiguration
        } yield {
          val isMarkdown = markdownFiles.matches(path)
          val isIgnore = config.ignores
            .map(ignore => {
              DEFAULT_FS.getPathMatcher(f"glob:${root}/${ignore}")
            })
            .foldLeft(false)((acc, matcher) => acc || matcher.matches(path))

          !isMarkdown || isIgnore
        }

      def collectionConfiguration: F[CollectionConfiguration] =
        for {
          ignores <- logger.action("read ignore file")(
            Effect[F].delay(
              Source.fromFile(f"${root}/${IGNORE_FILE}").getLines().toList
            )
          )
        } yield {
          CollectionConfiguration(new File(root), ignores = ignores)
        }

      def repositoryConfiguration: F[RepositoryConfiguration] =
        RepositoryConfiguration.DEFAULT.pure[F]

      def graphConfiguration: F[GraphConfiguration] =
        GraphConfiguration.DEFAULT.pure[F]
    }
}
