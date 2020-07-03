package mindmap.effect.generator

import cats.Applicative
import cats.data.Chain
import cats.implicits._
import java.net.URI
import org.apache.commons.io.FilenameUtils
import org.apache.log4j.Logger
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import mindmap.model.Collection
import mindmap.model.ResolvedLink
import mindmap.model.Tag
import mindmap.model.UnresolvedLink
import mindmap.model.Zettelkasten
import mindmap.model.generator.GeneratorAlgebra

class MarkdownGenerator[F[_]: Applicative[?[_]]] extends GeneratorAlgebra[F] {
  private def logger = Logger.getLogger(this.getClass())

  private def parseTags(content: String): Set[Tag] = {
    val firstLine = content.takeWhile(_ != '\n')
    val pattern = "^:[\\w\\-:]+:$".r
    pattern.findFirstIn(firstLine) match {
      case Some(s) => s.split(':').filter(_.nonEmpty).map(Tag(_)).toSet
      case None => Set()
    }
  }

  private def parseLinks(content: String): Chain[UnresolvedLink] = {
    val pattern = "\\[([^\\]]+)\\]\\(([^\\)]+)\\)".r
    val matches = pattern.findAllMatchIn(content)

    val links = matches
      .filter(_.groupCount >= 2)
      .map(m => (m.group(1), m.group(2)))
      .map { case (text, link) => UnresolvedLink(link) }
      .filter(link => {
        val uri = Try(new URI(link.to))
        val validScheme = uri match {
          case Success(u) => u.getScheme() != "http" && u.getScheme() != "https"
          case Failure(_) => true
        }
        val validExt = uri match {
          case Success(u) => {
            val ext = FilenameUtils.getExtension(u.getPath())
            ext.size == 0 || ext == "html" || ext == "md"
          }
          case Failure(_) => true
        }

        validScheme && validExt && !link.to.startsWith("#")
      })
      .toSeq

    Chain.fromSeq(links)
  }

  def generate(collection: Collection): F[Zettelkasten] = {
    val tags = Chain.fromSeq(
      collection.notes
        .map(note => parseTags(note.content))
        .foldLeft(Set[Tag]())(_ ++ _)
        .toSeq
    )
    val noteLinks = collection.notes
      .map(note => (note, parseLinks(note.content)))
      .flatMap {
        case (note, unresolvedLinks) => {
          unresolvedLinks
            .map(unresolved =>
              (unresolved, collection.notes.find(_.title == unresolved.to))
            )
            .filter {
              case (unresolved, resolved) => {
                if (!resolved.isDefined) {
                  logger.info(
                    f"Could not resolve link for ${unresolved} in ${note.title}"
                  )
                }

                resolved.isDefined
              }
            }
            .map {
              case (unresolved, resolved) => ResolvedLink(note, resolved.get)
            }
        }
      }

    val tagLinks: Chain[ResolvedLink] = collection.notes
      .map(note => {
        Chain.fromSeq(
          parseTags(note.content).map(tag => ResolvedLink(tag, note)).toSeq
        )
      })
      .flatten
    val links = noteLinks ++ tagLinks
    Zettelkasten(notes = collection.notes, links = links, tags = tags).pure[F]
  }
}
