package mindmap.model.configuration

trait ConfigurationAlgebra[F[_]]
    extends CollectionConfigurationAlgebra[F]
    with RepositoryConfigurationAlgebra[F]
    with NetworkConfigurationAlgebra[F]

object ConfigurationAlgebra {
  def apply[F[_]](implicit
    instance: ConfigurationAlgebra[F]
  ): ConfigurationAlgebra[F] = instance
}
