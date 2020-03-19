package mindmap.model.parser

import mindmap.model.Note

trait NoteParserAlgebra[F[_]]
    extends ContentParserAlgebra[F]
    with LinkParserAlgebra[F]
    with MetadataParserAlgebra[F]
    with TagParserAlgebra[F] {
  def parseNote(): F[Note]
}
