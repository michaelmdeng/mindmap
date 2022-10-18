package mindmap.effect.parser

import cats.MonadError
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

import mindmap.effect.Logging
import mindmap.effect.parser.LoggingParsers
import mindmap.model.Collection
import mindmap.model.Repository
import mindmap.model.Tag
import mindmap.model.UnresolvedLink
import mindmap.model.configuration.ConfigurationAlgebra
import mindmap.model.parser.RepositoryParserAlgebra
import mindmap.model.parser.markdown.BlockParsers
import mindmap.model.parser.markdown.TagBlock

class CommonMarkRepositoryParser[F[_]: ContextShift[?[_]]: Effect[?[_]]: Parallel[
  ?[_]
]: ConfigurationAlgebra[?[_]]]
    extends RepositoryParserAlgebra[F] {
  private class LinkCollector extends AbstractVisitor {
    var links: List[UnresolvedLink] = List()

    override def visit(link: Link): Unit = {
      links = links :+ UnresolvedLink(link.getDestination())
    }
  }

  private implicit val logger = new Logging(this.getClass())
  private val blockParsers = new BlockParsers() with LoggingParsers {}

  private def parseTags(content: String): F[Set[Tag]] =
    for {
      result <- blockParsers.parseAndLog(blockParsers.tags, content, "tags")
    } yield {
      result match {
        case blockParsers.Success(b, _) => b.tags.toSet
        case e: blockParsers.NoSuccess => Set[Tag]()
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
    val parser = Parser.builder().build();
    val node = parser.parse(content)
    val collector = new LinkCollector()
    node.accept(collector)
    collector.links.filter(isInternalLink(_)).pure[F]
  }

  def parseRepository(collection: Collection): F[Repository] =
    for {
      noteTags <- collection.notes
        .map(note => {
          MonadError[F, Throwable].tuple2(
            note.pure[F],
            logger.action(f"parse tags for note: ${note.title}", Level.DEBUG)(
              parseTags(note.content)
            )
          )
        })
        .parSequence
      noteLinks <- collection.notes
        .map(note => {
          MonadError[F, Throwable]
            .tuple2(
              note.pure[F],
              logger
                .action(f"parse links for note: ${note.title}", Level.DEBUG)(
                  parseLinks(note.content)
                )
            )
        })
        .parSequence
      config <- ConfigurationAlgebra[F].repositoryConfiguration
    } yield {
      Repository(
        noteTags = noteTags.toMap.map {
          case (note, tags) =>
            (
              note,
              tags.filter(tag => !config.excludeTags.contains(tag))
            )
        },
        noteLinks = noteLinks.toMap
      )
    }
}
