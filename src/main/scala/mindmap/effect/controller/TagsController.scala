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

import mindmap.model.ZettelkastenAlgebra
import mindmap.model.Note
import mindmap.model.Tag

class TagsController[
  F[_]: Defer[*[_]]: MonadError[*[_], Throwable]: Logging.Make
](
  zettel: ZettelkastenAlgebra[F]
) extends Http4sDsl[F] {
  private implicit val formats = Serialization.formats(NoTypeHints)
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

  private def index(): F[Response[F]] = for {
    tags <- zettel.tags()
    resp <- Ok(Serialization.write(tags))
  } yield (resp)

  private def find(id: Long): F[Response[F]] = for {
    tagOpt <- zettel.findTag(id)
    tag <- MonadError[F, Throwable]
      .fromOption(
        tagOpt,
        new Exception(f"Cannot find tag with id: $id")
      )
    notes <- zettel.getTagNotes(tag)
    template = html.tag(tag.name, notes)
    resp <- Ok(template.body, Header("Content-Type", "text/html"))
  } yield (resp)

  def routes(): HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root => index()
    case GET -> Root / name => tag(name)
    case GET -> Root / "find" / LongVar(id) => find(id)
  }
}
