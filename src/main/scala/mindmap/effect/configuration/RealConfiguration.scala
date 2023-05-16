package mindmap.effect.configuration

import cats.effect.Effect
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.PathMatcher
import scala.io.Source
import tofu.logging.Logging
import tofu.syntax.logging._

import mindmap.model.Tag
import mindmap.model.configuration.CollectionConfiguration
import mindmap.model.configuration.ConfigurationAlgebra
import mindmap.model.configuration.GraphConfiguration
import mindmap.model.configuration.RepositoryConfiguration

object RealConfiguration {
  private val IGNORE_FILE = ".mindmapignore"
  private val DEFAULT_FS = FileSystems.getDefault()

  def apply[F[_]: Effect[*[_]]: Logging.Make](
    r: String
  ): ConfigurationAlgebra[F] =
    new ConfigurationAlgebra[F] {
      private implicit val log: Logging[F] =
        Logging.Make[F].forService[ConfigurationAlgebra[F]]

      private val markdownFiles: PathMatcher =
        DEFAULT_FS.getPathMatcher(f"glob:${r}/**.md")
      private val rootPath: File = new File(r)
      private val collectionPath: File = new File(rootPath, "wiki")
      private val notePath: File = new File(rootPath, "html")

      def root(): F[File] = collectionConfiguration.map(_.root)
      def maxDepth(): F[Int] = collectionConfiguration.map(_.depth)

      def isIgnoreFile(path: Path): F[Boolean] =
        for {
          config <- collectionConfiguration
        } yield {
          val isMarkdown = markdownFiles.matches(path)
          val isIgnore = config.ignores
            .map(ignore => {
              DEFAULT_FS
                .getPathMatcher(f"glob:${collectionPath.getPath()}/${ignore}")
            })
            .foldLeft(false)((acc, matcher) => acc || matcher.matches(path))

          !isMarkdown || isIgnore
        }

      private def collectionConfiguration: F[CollectionConfiguration] =
        for {
          ignoreExists <- Effect[F].delay(
            new File(f"${collectionPath.getPath()}/${IGNORE_FILE}").exists()
          )
          ignores <-
            if (!ignoreExists) {
              Effect[F].pure(List())
            } else {
              info"read ignore file" >>
                Effect[F].delay(
                  Source
                    .fromFile(f"${collectionPath.getPath()}/${IGNORE_FILE}")
                    .getLines()
                    .toList
                )
            }
        } yield {
          CollectionConfiguration(collectionPath, ignores = ignores)
        }

      def isIgnoreTag(tag: Tag): F[Boolean] =
        for {
          config <- RepositoryConfiguration.DEFAULT.pure[F]
        } yield {
          config.excludeTags.contains(tag)
        }

      private def graphConfiguration: F[GraphConfiguration] =
        GraphConfiguration.DEFAULT.pure[F]

      def clusteringEnabled(): F[Boolean] =
        graphConfiguration.map(_.clusterEnabled)
      def clusterThreshold(): F[Int] =
        graphConfiguration.map(_.clusterThreshold)
      def isIgnoreClusterTag(tag: Tag): F[Boolean] =
        graphConfiguration.map(_.excludeClusterTags.contains(tag))
    }
}
