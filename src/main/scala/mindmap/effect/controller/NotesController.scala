package mindmap.effect.controller

import cats.Defer
import cats.MonadError
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.http4s.Header
import org.http4s.HttpRoutes
import org.http4s.Response
import org.http4s.dsl.Http4sDsl
import org.json4s.NoTypeHints
import org.json4s.native.Serialization
import play.twirl.api.HtmlFormat

import mindmap.model.ZettelkastenAlgebra

class NotesController[F[_]: Defer[*[_]]: MonadError[*[_], Throwable]](
  zettelRepository: ZettelkastenAlgebra[F]
) extends Http4sDsl[F] {
  implicit val formats = Serialization.formats(NoTypeHints)

  private def parse(content: String): F[String] = {
    val parser = Parser.builder().build()
    val node = parser.parse(content)
    val renderer = HtmlRenderer.builder().build()
    renderer.render(node).pure[F]
  }

  private def get(name: String): F[Response[F]] =
    for {
      noteOpt <- zettelRepository.getNote(name)
      note <- MonadError[F, Throwable].fromOption(
        noteOpt,
        new Exception(f"Cannot find note $name")
      )
      c <- parse(note.content)
      template = html.note(note.title, HtmlFormat.raw(c))
      resp <- Ok(template.body, Header("Content-Type", "text/html"))
    } yield (resp)

  private def index(): F[Response[F]] =
    for {
      notes <- zettelRepository.notes()
      resp <- Ok(Serialization.write(notes))
    } yield (resp)

  private def find(id: Long): F[Response[F]] =
    for {
      noteOpt <- zettelRepository.findNote(id)
      note <- MonadError[F, Throwable].fromOption(
        noteOpt,
        new Exception(f"Cannot find note with id: $id")
      )
      c <- parse(note.content)
      template = html.note(note.title, HtmlFormat.raw(c))
      resp <- Ok(template.body, Header("Content-Type", "text/html"))
    } yield (resp)

  def routes(): HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root => index()
    case GET -> Root / name => get(name)
    case GET -> Root / "find" / LongVar(id) => find(id)
  }
}
