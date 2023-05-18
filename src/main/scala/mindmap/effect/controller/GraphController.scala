package mindmap.effect.controller

import cats.Applicative
import cats.Defer
import cats.syntax.applicative._
import org.http4s.Header
import org.http4s.HttpRoutes
import org.http4s.Response
import org.http4s.dsl.Http4sDsl
import org.json4s.NoTypeHints
import org.json4s.native.Serialization
import tofu.logging.Logging
import tofu.syntax.logging._

import mindmap.model.graph.Network

class GraphController[F[_]: Applicative[*[_]]: Defer[*[_]]: Logging.Make](
  network: Network
) extends Http4sDsl[F] {
  private implicit val log: Logging[F] =
    Logging.Make[F].forService[GraphController[F]]

  private def nodes(): F[Response[F]] = {
    val nodes = {
      implicit val formats = Serialization.formats(NoTypeHints)
      Serialization.write(network.nodes)
    }

    Ok(nodes)
  }

  private def edges(): F[Response[F]] = {
    val edges = {
      implicit val formats = Serialization.formats(NoTypeHints)
      Serialization.write(network.edges)
    }

    Ok(edges)
  }

  def routes(): HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "nodes" => nodes()
    case GET -> Root / "edges" => edges()
  }
}
