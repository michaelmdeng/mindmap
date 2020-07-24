package mindmap.model.configuration

import java.io.File

trait ConfigurationAlgebra[F[_]] extends IgnoreAlgebra[F] {
  def rootDir: F[File]
}
