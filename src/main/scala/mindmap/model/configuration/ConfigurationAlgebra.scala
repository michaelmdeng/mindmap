package mindmap.model.configuration

import java.io.File

trait ConfigurationAlgebra[F[_]] extends IgnoreAlgebra[F] {
  def collectionConfiguration: F[CollectionConfiguration]

  def repositoryConfiguration: F[RepositoryConfiguration]

  def graphConfiguration: F[GraphConfiguration]
}

object ConfigurationAlgebra {
  def apply[F[_]](
    implicit instance: ConfigurationAlgebra[F]
  ): ConfigurationAlgebra[F] = instance
}
