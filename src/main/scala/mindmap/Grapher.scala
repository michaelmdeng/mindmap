package mindmap

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Resource
import cats.implicits._
import java.io.PrintWriter
import org.apache.logging.log4j.Level
import org.json4s.NoTypeHints
import org.json4s.native.Serialization
import org.json4s.native.Serialization.write

import mindmap.effect.Logging
import mindmap.effect.configuration.RealConfiguration
import mindmap.effect.graph.GraphGenerator
import mindmap.effect.graph.RealGraphWarnings
import mindmap.effect.parser.CommonMarkRepositoryParser
import mindmap.effect.parser.MarkdownZettelkastenParser
import mindmap.effect.parser.RealRepositoryWarnings
import mindmap.effect.parser.VimwikiCollectionParser
import mindmap.model.configuration.ConfigurationAlgebra

import org.commonmark.node._
import org.commonmark.parser.Parser

object Grapher extends IOApp {
  private implicit val logger: Logging[IO] = new Logging(Grapher.getClass())

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
      rootPath <- if (args.size > 0) {
        args(0).pure[IO]
      } else {
        "/home/mdeng/MyDrive/vimwiki".pure[IO]
      }
      implicit0(config: ConfigurationAlgebra[IO]) <- RealConfiguration[IO](
        rootPath
      ).pure[IO]
      collection <- IO.shift *> logger.action("parse collection")(
        new VimwikiCollectionParser[IO]().parseCollection(config)
      )
      repository <- IO.shift *> logger.action("parse repository")(
        new CommonMarkRepositoryParser[IO]().parseRepository(collection)
      )
      repoWarnings <- new RealRepositoryWarnings[IO]().warnings(repository)
      _ <- IO.shift *> logger.action("generate repository warnings")(
        repoWarnings.toList.map(warning => logger.warn(warning)).sequence
      )
      zettel <- IO.shift *> logger.action("parse Zettelkasten")(
        new MarkdownZettelkastenParser[IO]().parseZettelkasten(repository)
      )
      graphGen = new GraphGenerator[IO](zettel)
      graph <- IO.shift *> logger.action("generate graph")(
        graphGen.graph()
      )
      warnings <- new RealGraphWarnings[IO].warnings(graph)
      _ <- IO.shift *> logger.action("generate graph warnings")(
        warnings.toList.map(warning => logger.warn(warning)).sequence
      )
      network <- IO.shift *> logger.action("generate network")(
        graphGen.network(graph)
      )
      nodes = network.nodes
      edges = network.edges
      clusterTags = network.clusterTags.map {
        case (n1, n2) => (n1.id.toString(), n2.id)
      }
      clusterNotes = network.clusterNotes.map {
        case (cluster, notes) => {
          (cluster.id.toString(), notes.map(_.id))
        }
      }
      _ <- IO.shift *> withPrinter("data/mindmap-nodes.js").use(writer => {
        logger.action("write data to data/mindmap-nodes.js")(IO {
          implicit val formats = Serialization.formats(NoTypeHints)
          writer.println(f"var nodes = ${write(nodes.toList)};")
        })
      })
      _ <- IO.shift *> withPrinter("data/mindmap-edges.js").use(writer => {
        logger.action("write data to data/mindmap-edges.js")(IO {
          implicit val formats = Serialization.formats(NoTypeHints)
          writer.println(f"var edges = ${write(edges.toList)};")
        })
      })
      _ <- IO.shift *> withPrinter("data/mindmap-clusters.js").use(writer => {
        logger.action("write data to data/mindmap-clusters.js")(
          IO {
            implicit val formats = Serialization.formats(NoTypeHints)
            writer.println(f"var clusterTags = ${write(clusterTags)};")
            writer.println(f"var clusterNotes = ${write(clusterNotes)};")
          }
        )
      })
    } yield (ExitCode.Success)
}
