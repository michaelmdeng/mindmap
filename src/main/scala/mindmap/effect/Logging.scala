package mindmap.effect

import cats.effect.Effect
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

class Logging[F[_]: Effect[?[_]]](clazz: Class[_]) {
  val logger: Logger = LogManager.getLogger(clazz)

  private def ignoreFailure(f: F[Unit]): F[Unit] =
    f.redeem(_ => Effect[F].unit, _ => Effect[F].unit)

  def debug(msg: String): F[Unit] =
    ignoreFailure(Effect[F].delay(logger.debug(msg)))

  def info(msg: String): F[Unit] =
    ignoreFailure(Effect[F].delay(logger.info(msg)))

  def warn(msg: String): F[Unit] =
    ignoreFailure(Effect[F].delay(logger.warn(msg)))

  def action[A](msg: String)(act: F[A]): F[A] =
    for {
      _ <- info(f"START ${msg}")
      a <- act
      _ <- info(f"END ${msg}")
    } yield (a)
}
