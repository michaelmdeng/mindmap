package mindmap.model.parser

import mindmap.model.Collection
import mindmap.model.configuration.ConfigurationAlgebra

trait CollectionParserAlgebra[F[_]] {
  def parseCollection(config: ConfigurationAlgebra[F]): F[Collection]
}
