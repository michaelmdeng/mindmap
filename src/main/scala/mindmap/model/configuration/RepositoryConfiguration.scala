package mindmap.model.configuration

case class RepositoryConfiguration(excludeTags: Set[String])

object RepositoryConfiguration {
  final val DEFAULT: RepositoryConfiguration =
    RepositoryConfiguration(
      Set("article", "book", "architecture", "apache")
    )
}
