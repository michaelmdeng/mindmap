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
import mindmap.effect.configuration.RealConfiguration

object Server extends IOApp {
  private implicit val logger: Logging[IO] = new Logging(Server.getClass())

  def createApiService(): HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case GET -> Root / "hello" / name =>
        Ok(s"Hello, $name!")
    }
  }

  def createFileService(blocker: Blocker): HttpRoutes[IO] = {
    fileService[IO](FileService.Config("public", blocker))
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
      files = createFileService(blocker)
      apis = createApiService()
      notes = createNotesService(blocker, config.root)
      app = Router("/api" -> apis, "/public" -> files, "/notes" -> notes).orNotFound
      server <- EmberServerBuilder
        .default[IO]
        .withHost("0.0.0.0")
        .withPort(8080)
        .withHttpApp(app)
        .build
    } yield (server)

  def run(args: List[String]): IO[ExitCode] = {
    for {
      rootPath <- if (args.size > 0) {
        args(0).pure[IO]
      } else {
        "/home/mdeng/MyDrive/vimwiki/wiki".pure[IO]
      }
      config <- RealConfiguration[IO](
        rootPath
      ).pure[IO]
      noteConfig <- config.noteConfiguration
      _ <- createServer(noteConfig)
        .use(_ => logger.info("Initialized server") *> IO.never)
        .as(ExitCode.Success)
    } yield (ExitCode.Success)
  }
}
