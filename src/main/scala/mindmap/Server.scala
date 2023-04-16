package mindmap

import cats.effect._
import cats.implicits._
import java.io.File
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server._
import org.http4s.server.staticcontent._
import org.http4s.syntax.all._
import play.twirl.api._

import mindmap.effect.Logging
import mindmap.effect.configuration.RealConfiguration
import mindmap.effect.controller.NotesController
import mindmap.effect.controller.TagsController
import mindmap.model.configuration.ConfigurationAlgebra

object Server extends IOApp {
  private implicit val logger: Logging[IO] = new Logging(Server.getClass())

  def createGraphService(blocker: Blocker): HttpRoutes[IO] = {
    fileService[IO](FileService.Config("public/graph", blocker))
  }

  def createNetworkService(blocker: Blocker): HttpRoutes[IO] = {
    fileService[IO](FileService.Config("public/network", blocker))
  }

  def createAssetsService(blocker: Blocker): HttpRoutes[IO] = {
    fileService[IO](FileService.Config("public/assets", blocker))
  }

  def createServer(config: ConfigurationAlgebra[IO]): Resource[IO, Server] =
    for {
      blocker <- Blocker[IO]
      (collection, _) <- Resource.eval(Grapher.graph(config))
      notes = new NotesController[IO](collection).routes()
      tags = new TagsController[IO](collection).routes()
      assets = createAssetsService(blocker)
      graph = createGraphService(blocker)
      network = createNetworkService(blocker)
      app = Router(
        "/assets" -> assets,
        "/graph" -> graph,
        "/network" -> network,
        "/notes" -> notes,
        "/tags" -> tags,
        "/" -> network
      ).orNotFound
      server <- EmberServerBuilder
        .default[IO]
        .withHost("0.0.0.0")
        .withPort(8080)
        .withHttpApp(app)
        .build
    } yield (server)

  def run(args: List[String]): IO[ExitCode] = {
    for {
      path <- if (args.size > 0) {
        args(0).pure[IO]
      } else {
        "/home/mdeng/MyDrive/vimwiki".pure[IO]
      }
      config = RealConfiguration[IO](path)
      _ <- createServer(config)
        .use(_ => logger.info("Initialized server") *> IO.never)
        .as(ExitCode.Success)
    } yield (ExitCode.Success)
  }
}
