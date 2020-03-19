package mindmap.effect.parser

import cats.effect.Effect
import cats.implicits._
import java.io.File

import mindmap.model.parser._

object VimwikiNoteParser {
  def apply[F[_]: Effect[?[_]]](file: File) = {
    val fileParser = new FileNoteParser[F](file)
    new CompoundNoteParser[F](
      contentParser = fileParser,
      metadataParser = fileParser,
      linkParser = new MarkdownLinkParser(fileParser.content()),
      tagParser = new VimwikiTagParser(fileParser.content())
    )
  }
}
