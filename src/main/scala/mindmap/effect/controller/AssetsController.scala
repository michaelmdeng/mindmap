package mindmap.effect.controller

import cats.Defer
import cats.effect.Blocker
import cats.effect.ContextShift
import cats.effect.Sync
import org.http4s.HttpRoutes
import org.http4s.Uri
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Location
import org.http4s.server.staticcontent._

class AssetsController[F[_]: ContextShift[*[_]]: Defer[*[_]]: Sync[*[_]]](
  blocker: Blocker
) extends Http4sDsl[F] {
  def assetsRoutes(): HttpRoutes[F] = {
    fileService[F](FileService.Config("public/assets", blocker))
  }

  def indexRoutes(): HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root =>
      PermanentRedirect(
        Location(Uri.unsafeFromString("/assets/html/index.html"))
      )
    case GET -> Root / "index.html" =>
      PermanentRedirect(
        Location(Uri.unsafeFromString("/assets/html/index.html"))
      )
  }
}
