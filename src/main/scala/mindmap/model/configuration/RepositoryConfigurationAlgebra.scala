package mindmap.model.configuration

import mindmap.model.Tag

trait RepositoryConfigurationAlgebra[F[_]] {
  def isIgnoreTag(tag: Tag): F[Boolean]

  def repositoryConfig(): F[RepositoryConfiguration]
}
