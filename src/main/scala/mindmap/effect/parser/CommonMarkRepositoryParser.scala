package mindmap.effect.parser

import cats.Parallel
import cats.effect.ContextShift
import cats.effect.Effect
import cats.instances.list._
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.parallel._
import java.net.URI
import org.apache.commons.io.FilenameUtils
import org.apache.logging.log4j.Level
import org.commonmark.node.AbstractVisitor
import org.commonmark.node.Link
import org.commonmark.node.Node
import org.commonmark.parser.Parser
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import tofu.logging.Logging
import tofu.syntax.logging._

import mindmap.model.Collection
import mindmap.model.Repository
import mindmap.model.Tag
import mindmap.model.UnresolvedLink
import mindmap.model.configuration.ConfigurationAlgebra
import mindmap.model.parser.RepositoryParserAlgebra
import mindmap.model.parser.markdown.BlockParsers
import mindmap.model.parser.markdown.TagBlock

class CommonMarkRepositoryParser[F[_]: ContextShift[*[_]]: Effect[
  *[_]
]: Parallel[
  *[_]
]: ConfigurationAlgebra[*[_]]: Logging.Make]
    extends RepositoryParserAlgebra[F] {
  private implicit val log: Logging[F] =
    Logging.Make[F].forService[CommonMarkRepositoryParser[F]]

  private class LinkCollector extends AbstractVisitor {
    var links: List[UnresolvedLink] = List()

    override def visit(link: Link): Unit = {
      links = links :+ UnresolvedLink(link.getDestination())
    }
  }

  private val blockParsers = new BlockParsers()

  private def parseTags(content: String): F[Set[String]] =
    for {
      result <- blockParsers.parse(blockParsers.tags, content).pure[F]
    } yield {
      result match {
        case blockParsers.Success(b, _) => b.tags.toSet
        case e: blockParsers.NoSuccess => Set[String]()
      }
    }

  private def isInternalLink(link: UnresolvedLink): Boolean = {
    val isValidScheme = (uri: URI) =>
      uri.getScheme() != "http" && uri.getScheme() != "https"
    val isValidExt = (uri: URI) => {
      val ext = FilenameUtils.getExtension(uri.getPath())
      ext.isEmpty() || ext == "html" || ext == "md"
    }

    (Try(new URI(link.to)))
      .map(uri => isValidScheme(uri) && isValidExt(uri))
      .getOrElse(true) && !link.to.startsWith("#")
  }

  private def parseLinks(content: String): F[List[UnresolvedLink]] = {
    val parser = Parser.builder().build()
    val node = parser.parse(content)
    val collector = new LinkCollector()
    node.accept(collector)
    collector.links.filter(isInternalLink(_)).pure[F]
  }

  def parseRepository(collection: Collection): F[Repository] =
    for {
      parsedTags: Set[Tag] <- collection.notes
        .map(note => {
          debug"parse tags for note: ${note.title}" >>
            parseTags(note.content)
        })
        .parSequence
        .map(_.flatten.toSet.map((tagStr: String) => Tag(tagStr)))
      filteredTags: Set[Tag] <- parsedTags.toList
        .parTraverseFilter(tag => {
          for {
            isIgnoreTag <- ConfigurationAlgebra[F].isIgnoreTag(tag)
          } yield {
            Option.when(!isIgnoreTag)(tag)
          }
        })
        .map(_.toSet)
      filteredTagsMap = filteredTags.map(tag => (tag.name, tag)).toMap
      (noteTags, noteLinks) <- (
        collection.notes
          .map(note => {
            Effect[F].tuple2(
              note.pure[F],
              for {
                tags <- debug"parse tags for note: ${note.title}" >>
                  parseTags(note.content)
                filteredTags = tags.flatMap(tag =>
                  if (filteredTagsMap.contains(tag)) {
                    Some(filteredTagsMap(tag))
                  } else {
                    None
                  }
                )
              } yield (filteredTags.toSet)
            )
          })
          .parSequence,
        collection.notes
          .map(note => {
            Effect[F]
              .tuple2(
                note.pure[F],
                debug"parse links for note: ${note.title}" >>
                  parseLinks(note.content)
              )
          })
          .parSequence
      ).parMapN((tags, links) => (tags, links))
    } yield {
      Repository(
        noteTags = noteTags.toMap,
        noteLinks = noteLinks.toMap
      )
    }
}
