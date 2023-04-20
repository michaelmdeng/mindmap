package mindmap.model.parser

import cats.Show

import mindmap.model.Note
import mindmap.model.Repository

trait RepositoryWarningAlgebra[F[_]] {
  def warnings(repository: Repository): F[Iterable[RepositoryWarning]]
}

sealed trait RepositoryWarning
case class UnresolvableLink(note: Note, target: String)
    extends RepositoryWarning

object RepositoryWarning {
  object instances {
    implicit val showForRepositoryWarning: Show[RepositoryWarning] =
      new Show[RepositoryWarning] {
        def show(warning: RepositoryWarning): String = warning match {
          case l: UnresolvableLink => showForUnresolvableLink.show(l)
        }
      }

    implicit val showForUnresolvableLink: Show[UnresolvableLink] =
      new Show[UnresolvableLink] {
        def show(link: UnresolvableLink): String =
          f"Could not resolve link to ${link.target} in ${link.note.title}"
      }

    implicit val ordering: Ordering[UnresolvableLink] = Ordering.by {
      case UnresolvableLink(note, target) => (note.title, target)
    }
  }
}
