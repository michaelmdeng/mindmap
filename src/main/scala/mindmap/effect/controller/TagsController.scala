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
import tofu.logging.Logging
import tofu.syntax.logging._

import mindmap.model.ZettelkastenAlgebra
import mindmap.model.Note
import mindmap.model.Tag

class TagsController[
  F[_]: Defer[*[_]]: MonadError[*[_], Throwable]: Logging.Make
](
  zettel: ZettelkastenAlgebra[F]
) extends Http4sDsl[F] {
  private implicit val log: Logging[F] =
    Logging.Make[F].forService[TagsController[F]]

  private def tag(name: String): F[Response[F]] = for {
    tagOpt <- zettel.getTag(name)
    tag <- MonadError[F, Throwable]
      .fromOption(
        tagOpt,
        new Exception(f"Cannot find tag $name")
      )
      .onError { case e =>
        error"Error: ${e.getMessage()}"
      }
    notes <- zettel.getTagNotes(tag)
    template = html.tag(name, notes)
    resp <- Ok(template.body, Header("Content-Type", "text/html"))
  } yield (resp)

  def routes(): HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / name =>
    tag(name)
  }
}
