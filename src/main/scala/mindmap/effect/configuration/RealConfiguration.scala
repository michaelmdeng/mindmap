package mindmap.effect.configuration

import cats.effect.Effect
import cats.syntax.applicative._
import java.io.File

import mindmap.model.configuration.ConfigurationAlgebra
import mindmap.model.configuration.GraphConfiguration

object RealConfiguration {
  def apply[F[_]: Effect[?[_]]](file: String): ConfigurationAlgebra[F] =
    new IgnoreFile[F] with ConfigurationAlgebra[F] {
      def rootDir: F[File] = (new File(file)).pure[F]

      def graphConfiguration: F[GraphConfiguration] =
        GraphConfiguration.DEFAULT.pure[F]
    }
}
