package mindmap.model.generator

import mindmap.model.Zettelkasten

trait ZettelkastenWarningAlgebra[F[_]] {
  val zettel: Zettelkasten

  def warnings: F[Iterable[String]]
}
