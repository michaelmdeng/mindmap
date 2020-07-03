package mindmap.model.parser

import mindmap.model.Note

trait NoteParserAlgebra[F[_]]
    extends ContentParserAlgebra[F]
    with MetadataParserAlgebra[F] {
  def parseNote(): F[Note]
}
