package mindmap

import cats.syntax.applicative._
import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import tofu.logging.Logging
import tofu.syntax.logging._
import tofu.Delay

import mindmap.model.configuration.MainArgs

object Main extends IOApp {
  private implicit val ioDelay: Delay[IO] = new Delay[IO] {
    def delay[A](fa: => A): IO[A] = IO.delay(fa)
  }
  private implicit val makeLogging: Logging.Make[IO] = Logging.Make.plain[IO]
  private implicit val log: Logging[IO] =
    Logging.Make[IO].forService[IOApp]

  def run(args: List[String]): IO[ExitCode] = {
    val parsed = MainArgs(args)
    for {
      className <- IO.fromOption(parsed.clazz.toOption) {
        new Exception("Invalid class name")
      }
      remaining <- parsed.remaining().pure[IO]
      code <- className match {
        case n if n == Grapher.getClass().getName().split('$').head =>
          Grapher.run(args.tail)
        case n if n == Server.getClass().getName().split('$').head =>
          Server.run(args.tail)
        case default =>
          error"Could not find class with name: $className".as(ExitCode.Error)
      }
    } yield (code)
  }
}
