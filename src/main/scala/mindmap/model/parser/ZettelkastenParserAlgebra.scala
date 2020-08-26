package mindmap.model.parser

import mindmap.model.Repository
import mindmap.model.Zettelkasten

trait ZettelkastenParserAlgebra[F[_]] {
  def parseZettelkasten(repository: Repository): F[Zettelkasten]
}
