package mindmap.model.parser

import mindmap.model.Repository

trait RepositoryWarningAlgebra[F[_]] {
  def warnings(repository: Repository): F[Iterable[String]]
}
