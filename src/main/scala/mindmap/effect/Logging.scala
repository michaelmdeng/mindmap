package mindmap.effect

import cats.effect.Effect
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

class Logging[F[_]: Effect[*[_]]](clazz: Class[_]) {
  val logger: Logger = LogManager.getLogger(clazz)

  private def ignoreFailure(f: F[Unit]): F[Unit] =
    f.redeem(_ => Effect[F].unit, _ => Effect[F].unit)

  private def log(msg: String, level: Level = Level.INFO): F[Unit] =
    ignoreFailure(Effect[F].delay(logger.log(level, msg)))

  def debug(msg: String): F[Unit] = log(msg, Level.DEBUG)
  def error(msg: String): F[Unit] = log(msg, Level.ERROR)
  def fatal(msg: String): F[Unit] = log(msg, Level.FATAL)
  def info(msg: String): F[Unit] = log(msg, Level.INFO)
  def trace(msg: String): F[Unit] = log(msg, Level.TRACE)
  def warn(msg: String): F[Unit] = log(msg, Level.WARN)

  def action[A](msg: String, level: Level = Level.DEBUG)(act: F[A]): F[A] =
    for {
      _ <- log(f"START ${msg}", level)
      a <- act
      _ <- log(f"END ${msg}", level)
    } yield (a)
}

object Logging {
  def apply[F[_]: Effect[*[_]]](clazz: Class[_]): Logging[F] =
    new Logging(clazz)
}
