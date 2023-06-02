package mindmap.effect.controller

import cats.Applicative
import cats.Defer
import cats.syntax.applicative._
import org.http4s.HttpRoutes
import org.http4s.Response
import org.http4s.dsl.Http4sDsl
import org.json4s.NoTypeHints
import org.json4s.native.Serialization
import tofu.logging.Logging
import tofu.syntax.logging._

import mindmap.model.configuration.CollectionConfiguration
import mindmap.model.configuration.NetworkConfiguration
import mindmap.model.configuration.RepositoryConfiguration

class ConfigurationController[F[_]: Applicative[*[_]]: Defer[
  *[_]
]: Logging.Make](
  collectionConfig: CollectionConfiguration,
  networkConfig: NetworkConfiguration,
  repositoryConfig: RepositoryConfiguration
) extends Http4sDsl[F] {
  private implicit val log: Logging[F] =
    Logging.Make[F].forService[NetworkController[F]]

  private implicit val formats = Serialization.formats(NoTypeHints)

  private def collection(): F[Response[F]] = Ok(
    Serialization.write(collectionConfig)
  )

  private def repository(): F[Response[F]] = Ok(
    Serialization.write(repositoryConfig)
  )

  private def network(): F[Response[F]] = Ok(Serialization.write(networkConfig))

  def routes(): HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "collection" => collection()
    case GET -> Root / "repository" => repository()
    case GET -> Root / "network" => network()
  }
}
