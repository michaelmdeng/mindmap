package mindmap.model.configuration

import java.io.File

trait CollectionConfigurationAlgebra[F[_]] extends IgnoreAlgebra[F] {
  def root(): F[File]

  def maxDepth(): F[Int]

  def collectionConfig(): F[CollectionConfiguration]
}
