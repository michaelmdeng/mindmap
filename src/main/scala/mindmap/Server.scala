package mindmap

import cats.effect.Blocker
import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Resource
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.Server
import org.http4s.server.staticcontent._
import org.http4s.syntax.all._
import tofu.logging.Logging
import tofu.syntax.logging._
import tofu.Delay

import mindmap.effect.configuration.RealConfiguration
import mindmap.effect.controller.NotesController
import mindmap.effect.controller.TagsController
import mindmap.effect.controller.WarningController
import mindmap.effect.zettelkasten.MemoryZettelkastenRepository
import mindmap.model.configuration.ConfigurationAlgebra
import mindmap.model.configuration.ServerArgs

object Server extends IOApp {
  private implicit val ioDelay: Delay[IO] = new Delay[IO] {
    def delay[A](fa: => A): IO[A] = IO.delay(fa)
  }
  private implicit val makeLogging: Logging.Make[IO] = Logging.Make.plain[IO]
  private implicit val log: Logging[IO] =
    Logging.Make[IO].forService[IOApp]

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
      (collection, graphWarnings, repoWarnings) <- Resource.eval(
        Grapher.graph(config)
      )
      zettelRepo = new MemoryZettelkastenRepository[IO](collection)
      _ <- Resource.eval(info"initialized zettelkasten service")
      (notes, tags, warnings) <- (
        Resource.eval(
          new NotesController[IO](zettelRepo).routes().pure[IO]
        ) <* Resource
          .eval(
            info"initialized notes controller"
          ),
        Resource.eval(
          new TagsController[IO](zettelRepo).routes().pure[IO]
        ) <* Resource
          .eval(
            info"initialized tags controller"
          ),
        Resource.eval(
          new WarningController[IO](graphWarnings, repoWarnings)
            .routes()
            .pure[IO]
        ) <* Resource.eval(info"initialized warning controller")
      ).parMapN((a, b, c) => (a, b, c))
      (assets, graph, network) <- (
        Resource.eval(createAssetsService(blocker).pure[IO]) <* Resource.eval(
          info"initialized assets service"
        ),
        Resource.eval(createGraphService(blocker).pure[IO]) <* Resource.eval(
          info"initialized graph service"
        ),
        Resource.eval(createNetworkService(blocker).pure[IO]) <* Resource.eval(
          info"initialized network service"
        )
      ).parMapN((a, b, c) => (a, b, c))
      app = Router(
        "/assets" -> assets,
        "/graph" -> graph,
        "/network" -> network,
        "/notes" -> notes,
        "/tags" -> tags,
        "/warnings" -> warnings,
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
      path <- IO.fromOption(ServerArgs(args).path.toOption) {
        new Exception("Invalid vimwiki path")
      }
      config = RealConfiguration[IO](path)
      _ <- createServer(config)
        .use(_ => info"Initialized server" *> IO.never)
        .as(ExitCode.Success)
    } yield (ExitCode.Success)
  }
}
