package mindmap.model.configuration

import mindmap.model.Tag

case class RepositoryConfiguration(excludeTags: Set[Tag])

object RepositoryConfiguration {
  final val DEFAULT: RepositoryConfiguration =
    RepositoryConfiguration(
      Set(Tag("article"), Tag("book"), Tag("architecture"), Tag("apache"))
    )
}
