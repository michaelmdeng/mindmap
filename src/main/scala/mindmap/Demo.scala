package mindmap

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Resource
import cats.effect.implicits._
import cats.implicits._
import java.io.File
import java.io.PrintWriter
import org.json4s.NoTypeHints
import org.json4s.native.Serialization
import org.json4s.native.Serialization.write

import mindmap.effect.generator.MarkdownGenerator
import mindmap.effect.graph.GraphGenerator
import mindmap.effect.parser.VimwikiCollection

object Demo extends IOApp {
  def withPrinter(file: String): Resource[IO, PrintWriter] =
    Resource(IO {
      val pw = new PrintWriter(file)
      (
        pw,
        IO {
          pw.flush()
          pw.close()
        }
      )
    })

  def run(args: List[String]): IO[ExitCode] =
    for {
      notes <- VimwikiCollection[IO](
        new File("/home/mdeng/Dropbox/vimwiki/wiki")
      )
      zettel <- new MarkdownGenerator[IO]().generate(notes)
      t <- new GraphGenerator[IO]().graph(zettel)
      _ <- withPrinter("data/mindmap-notes").use(notesWriter => {
        for {
          _ <- IO {
            zettel.notes.map(note => notesWriter.println(note.title))
          }
          _ <- withPrinter("data/mindmap-tags").use(tagsWriter => {
            IO {
              zettel.tags.map(tag => tagsWriter.println(tag.name))
            }
          })
        } yield ()
      })
      _ <- withPrinter("data/mindmap-data.js").use(writer => {
        IO {
          val nodes = t._1
          val edges = t._2
          implicit val formats = Serialization.formats(NoTypeHints)
          writer.println(f"var nodes = ${write(nodes.toList)};")
          writer.println(f"var edges = ${write(edges.toList)};")
        }
      })
    } yield (ExitCode.Success)
}
