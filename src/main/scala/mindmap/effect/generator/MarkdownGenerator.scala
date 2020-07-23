package mindmap.effect.generator

import cats.MonadError
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
import mindmap.model.parser.markdown.BlockParsers
import mindmap.model.parser.markdown.BlockQuote
import mindmap.model.parser.markdown.Header
import mindmap.model.parser.markdown.LinkParsers
import mindmap.model.parser.markdown.Paragraph
import mindmap.model.parser.markdown.TagBlock
import mindmap.model.parser.markdown.TextParagraph

class MarkdownGenerator[F[_]: MonadError[?[_], Throwable]]
    extends GeneratorAlgebra[F] {
  private def logger = Logger.getLogger(this.getClass())

  private def parseParagraphs(content: String): F[List[Paragraph]] = {
    val blockParsers = new BlockParsers()
    blockParsers.parse(blockParsers.blocks, content) match {
      case blockParsers.Success(bs, _) => blockParsers.mergeBlocks(bs).pure[F]
      case e: blockParsers.NoSuccess =>
        (new Exception(e.msg)).raiseError[F, List[Paragraph]]
    }
  }

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

    (Try(new URI(link.to)) match {
      case Success(uri) => isValidScheme(uri) && isValidExt(uri)
      case Failure(_) => true
    }) && !link.to.startsWith("#")
  }

  private def parseLinks(content: String): F[List[UnresolvedLink]] =
    for {
      _ <- MonadError[F, Throwable].unit
      paragraphs <- parseParagraphs(content)
      linkParsers = new LinkParsers()
      links <- paragraphs
        .map(paragraph => {
          val getParagraphLinks = (s: String) => {
            linkParsers.parse(linkParsers.links, s) match {
              case linkParsers.Success(ls, _) => ls.pure[F]
              case e: linkParsers.NoSuccess =>
                (new Exception(e.msg)).raiseError[F, List[UnresolvedLink]]
            }
          }
          paragraph match {
            case TextParagraph(s) => getParagraphLinks(s)
            case Header(h, _) => getParagraphLinks(h)
            case BlockQuote(q) => getParagraphLinks(q)
            case _ => List[UnresolvedLink]().pure[F]
          }
        })
        .sequence
        .map(_.flatten)
    } yield {
      links.filter(isInternalLink(_))
    }

  def generate(collection: Collection): F[Zettelkasten] =
    for {
      noteTags <- collection.notes
        .map(note => {
          MonadError[F, Throwable].tuple2(note.pure[F], parseTags(note.content))
        })
        .sequence
      noteLinks <- collection.notes
        .map(note => {
          MonadError[F, Throwable]
            .tuple2(note.pure[F], parseLinks(note.content))
            .map {
              case (note, unresolvedLinks) => {
                unresolvedLinks
                  .mapFilter(unresolved => {
                    val resolvedNote =
                      collection.notes.find(_.title == unresolved.to)
                    resolvedNote match {
                      case Some(resolved) => Some(ResolvedLink(note, resolved))
                      case None => {
                        logger.info(
                          f"Could not resolve link for ${unresolved} in ${note.title}"
                        )
                        None
                      }
                    }
                  })
              }
            }
        })
        .sequence
        .map(_.flatten)
    } yield {
      val tagLinks = noteTags.flatMap {
        case (note, tags) => tags.map(ResolvedLink(_, note))
      }
      val tags = noteTags
        .map {
          case (_, tags) => tags
        }
        .reduce(_ ++ _)
      val links = noteLinks ++ tagLinks

      Zettelkasten(notes = collection.notes, links = links, tags = tags)
    }
}
