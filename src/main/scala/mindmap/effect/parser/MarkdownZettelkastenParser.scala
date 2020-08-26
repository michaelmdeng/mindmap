package mindmap.effect.parser

import cats.effect.Effect
import cats.instances.list._
import cats.syntax.applicative._
import cats.syntax.functorFilter._

import mindmap.effect.Logging
import mindmap.model.Repository
import mindmap.model.ResolvedLink
import mindmap.model.Tag
import mindmap.model.Zettelkasten
import mindmap.model.parser.ZettelkastenParserAlgebra

class MarkdownZettelkastenParser[F[_]: Effect[?[_]]]
    extends ZettelkastenParserAlgebra[F] {
  private implicit val logger = new Logging(this.getClass())

  def parseZettelkasten(repository: Repository): F[Zettelkasten] = {
    val notes = repository.noteTags.keySet.toList

    val tags = repository.noteTags.foldLeft(Set[Tag]())((acc, t) => {
      t match {
        case (_, tags) => acc ++ tags
      }
    })

    val tagLinks = repository.noteTags.flatMap {
      case (note, tags) => tags.map(ResolvedLink(_, note))
    }
    val noteLinks = repository.noteLinks.flatMap {
      case (note, unresolvedLinks) => {
        unresolvedLinks
          .mapFilter(unresolved => {
            notes.find(_.title == unresolved.to).map(ResolvedLink(note, _))
          })
      }
    }
    val links = (noteLinks ++ tagLinks).toList

    Zettelkasten(notes = notes, tags = tags, links = links).pure[F]
  }
}
