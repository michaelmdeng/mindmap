package mindmap.effect.server

import cats.effect._
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.server.blaze._
import org.http4s.syntax._
import org.json4s.NoTypeHints
import org.json4s.native.Serialization
import org.json4s.native.Serialization.read
import org.json4s.native.Serialization.write
import java.io.File
import org.http4s.json4s.Json4sInstances

import mindmap.effect._
import mindmap.effect.generator._
import mindmap.effect.graph._
import mindmap.effect.parser._
import mindmap.model._

// object Server extends IOApp {
//   implicit val formats = Serialization.formats(NoTypeHints)
//   implicit val nodesEncoder = Serialization.formats(NoTypeHints)

//   def run(args: List[String]): IO[ExitCode] =
//     for {
//       notes <- VimwikiCollection[IO](
//         new File("/home/mdeng/Dropbox/vimwiki/wiki")
//       )
//       zettel <- new MarkdownGenerator[IO]().generate(notes)
//       t <- new GraphGenerator[IO]().graph(zettel)
//       nodes = t._1
//       edges = t._2
//       routes = HttpRoutes
//         .of[IO] {
//           case GET -> Root / "node" => Ok(nodes.toList)
//           case GET -> Root / "edge" => Ok(edges.toList)
//         }
//         .orNotFound

//       server <- BlazeServerBuilder[IO]
//         .bindHttp(8080, "localhost")
//         .withHttpApp(routes)
//         .serve
//         .compile
//         .drain
//         .as(ExitCode.Success)
//     } yield (server)
// }
