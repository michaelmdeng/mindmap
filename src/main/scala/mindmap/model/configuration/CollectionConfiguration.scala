package mindmap.model.configuration

import java.io.File

case class CollectionConfiguration(
  root: File,
  ignores: List[String],
  depth: Int = CollectionConfiguration.DEFAULT_MAX_DEPTH
)

object CollectionConfiguration {
  final val DEFAULT_MAX_DEPTH: Int = 5
}
