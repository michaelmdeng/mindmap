package mindmap.effect

import cats.data.Chain
import cats.effect.Effect
import cats.implicits._
import java.io.File
import java.nio.file.Files
import scala.jdk.StreamConverters._

import mindmap.model.Note
import mindmap.effect.parser.VimwikiNoteParser

object VimwikiIndex extends {
  private def getFiles[F[_]: Effect[?[_]]](dir: File): F[Chain[File]] =
    Effect[F].delay {
      Chain.fromSeq(
        Files
          .walk(dir.toPath())
          .toScala(Seq)
          .filter(Files.isRegularFile(_))
          .map(_.toFile())
      )
    }

  def apply[F[_]: Effect[?[_]]](dir: File): F[Chain[Note]] =
    for {
      files <- getFiles(dir)
      _ = {
        println(files)
      }
      notes <- files
        .map(file => {
          VimwikiNoteParser(file)
            .parseNote()
            .map(Chain(_))
            .handleError(e => Chain())
        })
        .sequence
        .map(_.flatten)
    } yield (notes)
}
