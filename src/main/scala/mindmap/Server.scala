package mindmap

import cats.effect._
import cats.implicits._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server._
import org.http4s.server.staticcontent._
import org.http4s.syntax.all._

import mindmap.effect.Logging

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

  def createServer(): Resource[IO, Server] =
    for {
      blocker <- Blocker[IO]
      files = createFileService(blocker)
      apis = createApiService()
      app = Router("/api" -> apis, "/public" -> files).orNotFound
      server <- EmberServerBuilder
        .default[IO]
        .withHost("0.0.0.0")
        .withPort(8080)
        .withHttpApp(app)
        .build
    } yield (server)

  def run(args: List[String]): IO[ExitCode] = {
    createServer()
      .use(_ => logger.info("Initialized server") *> IO.never)
      .as(ExitCode.Success)
  }
}
