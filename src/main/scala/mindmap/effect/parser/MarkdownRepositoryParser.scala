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
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import mindmap.effect.Logging
import mindmap.effect.parser.LoggingParsers
import mindmap.model.Collection
import mindmap.model.Repository
import mindmap.model.Tag
import mindmap.model.UnresolvedLink
import mindmap.model.parser.RepositoryParserAlgebra
import mindmap.model.parser.markdown.BlockParsers
import mindmap.model.parser.markdown.BlockQuote
import mindmap.model.parser.markdown.Header
import mindmap.model.parser.markdown.LinkParsers
import mindmap.model.parser.markdown.Paragraph
import mindmap.model.parser.markdown.TagBlock
import mindmap.model.parser.markdown.TextParagraph

class MarkdownRepositoryParser[F[_]: ContextShift[?[_]]: Effect[?[_]]: Parallel[
  ?[_]
]] extends RepositoryParserAlgebra[F] {
  private implicit val logger = new Logging(this.getClass())

  private val blockParsers = new BlockParsers() with LoggingParsers {}
  private val linkParsers = new LinkParsers() with LoggingParsers {}

  private def parseParagraphs(content: String): F[List[Paragraph]] =
    for {
      result <- blockParsers.parseAndLog(blockParsers.blocks, content, "blocks")
      paragraphs <- result match {
        case blockParsers.Success(bs, _) => blockParsers.mergeBlocks(bs).pure[F]
        case e: blockParsers.NoSuccess =>
          (new Exception(e.msg)).raiseError[F, List[Paragraph]]
      }
    } yield (paragraphs)

  private def parseTags(content: String): F[Set[Tag]] =
    for {
      paragraphs <- parseParagraphs(content)
    } yield {
      paragraphs
        .map(paragraph => {
          paragraph match {
            case TagBlock(ts) => ts.toSet
            case _ => Set[Tag]()
          }
        })
        .reduce(_ ++ _)
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

  private def parseParagraphLinks(paragraph: String): F[List[UnresolvedLink]] =
    for {
      result <- linkParsers.parseAndLog(linkParsers.links, paragraph, "links")
      links <- result match {
        case linkParsers.Success(ls, _) => ls.pure[F]
        case e: linkParsers.NoSuccess =>
          (new Exception(e.msg)).raiseError[F, List[UnresolvedLink]]
      }
    } yield (links)

  private def parseLinks(content: String): F[List[UnresolvedLink]] = {
    for {
      paragraphs <- parseParagraphs(content)
      links <- paragraphs
        .map(paragraph => {
          paragraph match {
            case TextParagraph(s) => parseParagraphLinks(s)
            case Header(h, _) => parseParagraphLinks(h)
            case BlockQuote(q) => parseParagraphLinks(q)
            case _ => List[UnresolvedLink]().pure[F]
          }
        })
        .parFlatSequence
    } yield (links.filter(isInternalLink(_)))
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
    } yield {
      Repository(noteTags = noteTags.toMap, noteLinks = noteLinks.toMap)
    }
}
