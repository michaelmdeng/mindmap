package mindmap.effect.controller

import cats.Applicative
import cats.Defer
import cats.instances.list._
import cats.syntax.functorFilter._
import org.http4s.Header
import org.http4s.HttpRoutes
import org.http4s.Response
import org.http4s.dsl.Http4sDsl

import mindmap.model.graph.GraphWarning
import mindmap.model.graph.GraphWarning.instances._
import mindmap.model.graph.OverlappingTags
import mindmap.model.graph.SingleNote
import mindmap.model.graph.SingleTag

class WarningController[F[_]: Applicative[*[_]]: Defer[*[_]]](
  warnings: Iterable[GraphWarning]
) extends Http4sDsl[F] {
  def get(): F[Response[F]] = {
    val singleTags = warnings.toList
      .mapFilter(warning =>
        warning match {
          case t: SingleTag => Some(t)
          case default => None
        }
      )
      .sorted
    val singleNotes = warnings.toList
      .mapFilter(warning =>
        warning match {
          case n: SingleNote => Some(n)
          case default => None
        }
      )
      .sorted
    val overlappingTags = warnings.toList
      .mapFilter(warning =>
        warning match {
          case t: OverlappingTags => Some(t)
          case default => None
        }
      )
      .sorted
    val template = html.warnings(
      singleTags.toList,
      singleNotes.toList,
      overlappingTags.toList
    )
    Ok(template.body, Header("Content-Type", "text/html"))
  }

  def routes(): HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root => get()
  }
}
