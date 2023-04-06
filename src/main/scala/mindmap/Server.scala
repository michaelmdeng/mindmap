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

import mindmap.effect.Logging
import mindmap.model.configuration.NoteConfiguration

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

  def createNotesService(
    blocker: Blocker,
    notesPath: File
  ): HttpRoutes[IO] = {
    fileService[IO](FileService.Config(notesPath.getPath(), blocker))
  }

  def createServer(config: NoteConfiguration): Resource[IO, Server] =
    for {
      blocker <- Blocker[IO]
      notes = createNotesService(blocker, config.root)
      assets = createAssetsService(blocker)
      graph = createGraphService(blocker)
      network = createNetworkService(blocker)
      app = Router(
        "/assets" -> assets,
        "/graph" -> graph,
        "/network" -> network,
        "/notes" -> notes,
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
      config = NoteConfiguration(new File(path))
      _ <- createServer(config)
        .use(_ => logger.info("Initialized server") *> IO.never)
        .as(ExitCode.Success)
    } yield (ExitCode.Success)
  }
}
