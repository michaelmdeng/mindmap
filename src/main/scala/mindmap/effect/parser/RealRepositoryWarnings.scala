package mindmap.effect.parser

import cats.Applicative
import cats.instances.list._
import cats.syntax.applicative._
import cats.syntax.functorFilter._

import mindmap.model.Repository
import mindmap.model.parser.RepositoryWarning
import mindmap.model.parser.UnresolvableLink
import mindmap.model.parser.RepositoryWarningAlgebra

class RealRepositoryWarnings[F[+_]: Applicative[*[_]]]
    extends RepositoryWarningAlgebra[F] {
  def warnings(repository: Repository): F[Iterable[RepositoryWarning]] = {
    val notes = repository.noteTags.keySet.toList

    repository.noteLinks
      .flatMap {
        case (note, unresolvedLinks) => {
          unresolvedLinks
            .mapFilter(unresolved => {
              notes.find(_.title == unresolved.to) match {
                case None => Some(UnresolvableLink(note, unresolved.to))
                case _ => None
              }
            })
        }
      }
      .pure[F]
  }
}
