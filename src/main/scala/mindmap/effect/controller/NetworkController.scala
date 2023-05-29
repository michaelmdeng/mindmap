package mindmap.effect.controller

import cats.Defer
import cats.MonadError
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._
import org.http4s.Header
import org.http4s.HttpRoutes
import org.http4s.Response
import org.http4s.dsl.Http4sDsl
import org.json4s.NoTypeHints
import org.json4s.native.Serialization
import tofu.logging.Logging
import tofu.syntax.logging._

import mindmap.model.Note
import mindmap.model.MindmapAlgebra
import mindmap.model.ZettelkastenAlgebra
import mindmap.model.graph.Network

class NetworkController[F[_]: MonadError[*[_], Throwable]: Defer[
  *[_]
]: Logging.Make](
  mindmap: MindmapAlgebra[F],
  zettel: ZettelkastenAlgebra[F]
) extends Http4sDsl[F] {
  private implicit val log: Logging[F] =
    Logging.Make[F].forService[NetworkController[F]]

  private def network(): F[Response[F]] = for {
    net <- mindmap.network()
    resp <- {
      implicit val formats = Serialization.formats(NoTypeHints)
      Ok(Serialization.write(net))
    }
  } yield (resp)

  private def noteNetwork(title: String): F[Response[F]] = for {
    noteOpt <- zettel.getNote(title)
    note <- MonadError[F, Throwable].fromOption(
      noteOpt,
      new Exception(s"Note $title not found")
    )
    sub <- mindmap.subnetwork(note)
    resp <- {
      implicit val formats = Serialization.formats(NoTypeHints)
      Ok(Serialization.write(sub))
    }
  } yield (resp)

  private def tagNetwork(name: String): F[Response[F]] = for {
    tagOpt <- zettel.getTag(name)
    tag <- MonadError[F, Throwable]
      .fromOption(
        tagOpt,
        new Exception(s"Tag $name not found")
      )
      .onError { case e =>
        error"Error: ${e.getMessage()}"
      }
    sub <- mindmap.subnetwork(tag)
    resp <- {
      implicit val formats = Serialization.formats(NoTypeHints)
      Ok(Serialization.write(sub))
    }
  } yield (resp)

  def routes(): HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root => network()
    case GET -> Root / "note" / title => noteNetwork(title)
    case GET -> Root / "tag" / title => tagNetwork(title)
  }
}
