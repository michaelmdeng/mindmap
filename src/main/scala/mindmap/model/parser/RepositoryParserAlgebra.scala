package mindmap.model.parser

import mindmap.model.Collection
import mindmap.model.Repository

trait RepositoryParserAlgebra[F[_]] {
  def parseRepository(collection: Collection): F[Repository]
}
