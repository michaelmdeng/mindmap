package mindmap.effect.configuration

import cats.effect.Effect
import cats.implicits._
import java.io.File

import mindmap.model.configuration.ConfigurationAlgebra

object RealConfiguration {
  def apply[F[_]: Effect[?[_]]](file: String): ConfigurationAlgebra[F] =
    new IgnoreFile[F] with ConfigurationAlgebra[F] {
      def rootDir: F[File] = (new File(file)).pure[F]
    }
}
