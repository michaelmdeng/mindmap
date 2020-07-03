package mindmap.model.generator

import mindmap.model.Collection
import mindmap.model.Zettelkasten

trait GeneratorAlgebra[F[_]] {
  def generate(collection: Collection): F[Zettelkasten]
}
