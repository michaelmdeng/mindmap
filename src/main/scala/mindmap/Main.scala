package mindmap

import cats.syntax.applicative._
import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp

import mindmap.effect.Logging

object Main extends IOApp {
  private implicit val logger: Logging[IO] = new Logging(Main.getClass())

  def run(args: List[String]): IO[ExitCode] = {
    // TODO better arg parsing
    val className: String = args(0)
    className match {
      case n if n == Grapher.getClass().getName() => Grapher.run(args.tail)
      case n if n == Server.getClass().getName() => Server.run(args.tail)
      case default => ExitCode.Error.pure[IO]
    }
  }
}
