package mindmap.model.parser

trait ContentParserAlgebra[F[_]] {
  def content(): F[String]
}
