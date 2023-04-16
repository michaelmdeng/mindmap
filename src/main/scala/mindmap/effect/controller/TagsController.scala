package mindmap.effect.controller

import cats.Defer
import cats.Monad
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import org.http4s.Header
import org.http4s.HttpRoutes
import org.http4s.Response
import org.http4s.dsl.Http4sDsl

import mindmap.model.Zettelkasten
import mindmap.model.Note
import mindmap.model.Tag

class TagsController[F[_]: Defer[*[_]]: Monad[*[_]]](
  zettel: Zettelkasten
) extends Http4sDsl[F] {
  private def tag(name: String): F[Response[F]] = {
    for {
      links <- zettel.links
        .filter(link => {
          link.from match {
            case t: Tag => t.name == name
            case default => false
          }
        })
        .pure[F]
      notes = links
        .flatMap(link => {
          link.to match {
            case n: Note => List(n)
            case default => List()
          }
        })
        .sortBy(_.title)
      template = html.tag(name, notes)
      resp <- Ok(template.body, Header("Content-Type", "text/html"))
    } yield (resp)
  }

  def routes(): HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / name => tag(name)
  }
}
