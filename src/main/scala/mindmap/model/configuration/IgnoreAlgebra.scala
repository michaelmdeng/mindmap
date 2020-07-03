package mindmap.model.configuration

import java.nio.file.Path

trait IgnoreAlgebra[F[_]] {
  def isIgnoreFile(path: Path): F[Boolean]
}
