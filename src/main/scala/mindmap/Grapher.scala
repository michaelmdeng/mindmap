package mindmap

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Resource
import cats.implicits._
import java.io.PrintWriter
import org.json4s.NoTypeHints
import org.json4s.native.Serialization
import org.json4s.native.Serialization.write
import tofu.logging.Logging
import tofu.syntax.logging._
import tofu.Delay

import mindmap.effect.configuration.RealConfiguration
import mindmap.effect.graph.GraphGenerator
import mindmap.effect.graph.RealGraphWarnings
import mindmap.effect.parser.CommonMarkRepositoryParser
import mindmap.effect.parser.MarkdownZettelkastenParser
import mindmap.effect.parser.RealRepositoryWarnings
import mindmap.effect.parser.VimwikiCollectionParser
import mindmap.model.configuration.ConfigurationAlgebra
import mindmap.model.Zettelkasten
import mindmap.model.parser.RepositoryWarning.instances._
import mindmap.model.parser.RepositoryWarning
import mindmap.model.graph.GraphWarning.instances._
import mindmap.model.graph.GraphWarning

object Grapher extends IOApp {
  private implicit val ioDelay: Delay[IO] = new Delay[IO] {
    def delay[A](fa: => A): IO[A] = IO.delay(fa)
  }
  private implicit val makeLogging: Logging.Make[IO] = Logging.Make.plain[IO]
  private implicit val log: Logging[IO] =
    Logging.Make[IO].forService[IOApp]

  def withPrinter(file: String): Resource[IO, PrintWriter] =
    Resource.make(debug"acquire writer to $file" >> IO(new PrintWriter(file)))(
      pw => {
        debug"close writer to $file" >> IO {
          pw.flush()
          pw.close()
        }
      }
    )

  def graph(
    config: ConfigurationAlgebra[IO]
  ): IO[(Zettelkasten, Iterable[GraphWarning], Iterable[RepositoryWarning])] =
    for {
      _ <- IO.unit
      implicit0(c: ConfigurationAlgebra[IO]) = config
      networkPath = "public/graph"
      collection <- info"parse collection" >>
        new VimwikiCollectionParser[IO]().parseCollection(config)
      repository <- info"parse repository" >>
        new CommonMarkRepositoryParser[IO]().parseRepository(collection)
      _ <- info"generate repository warnings"
      repoWarnings <- new RealRepositoryWarnings[IO]().warnings(repository)
      _ <- repoWarnings.toList
        .map(warning => {
          warn"${warning.show}"
        })
        .parSequence
      zettel <- info"parse zettelkasten" >>
        new MarkdownZettelkastenParser[IO]().parseZettelkasten(repository)
      graphGen = new GraphGenerator[IO](zettel)
      graph <- info"generate graph" >> graphGen.graph()
      warnings <- info"generate graph warnings" >> new RealGraphWarnings[IO]
        .warnings(graph)
      _ <- warnings.toSeq
        .map(warning => {
          warn"${warning.show}"
        })
        .parSequence
      network <- info"generate network" >> graphGen.network(graph)
      nodes = network.nodes
      edges = network.edges
      _ <- Seq(
        withPrinter(f"$networkPath/mindmap-nodes.js").use(writer => {
          info"write public/mindmap-nodes.js" >> IO {
            implicit val formats = Serialization.formats(NoTypeHints)
            writer.println(f"var nodes = ${write(nodes.toList)};")
          }
        }),
        withPrinter(f"$networkPath/mindmap-edges.js").use(writer => {
          info"write public/mindmap-edges.js" >> IO {
            implicit val formats = Serialization.formats(NoTypeHints)
            writer.println(f"var edges = ${write(edges.toList)};")
          }
        })
      ).parSequence
    } yield ((zettel, warnings, repoWarnings))

  def run(args: List[String]): IO[ExitCode] =
    for {
      rootPath <-
        if (args.size > 0) {
          args(0).pure[IO]
        } else {
          "/home/mdeng/MyDrive/vimwiki".pure[IO]
        }
      networkPath <-
        if (args.size > 1) {
          args(1).pure[IO]
        } else {
          "public/graph".pure[IO]
        }
      _ <- graph(RealConfiguration[IO](rootPath))
    } yield (ExitCode.Success)
}
