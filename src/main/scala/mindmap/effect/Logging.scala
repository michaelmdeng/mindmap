package mindmap.effect

import cats.implicits._
import cats.effect.Effect
import java.lang.Class
import org.apache.log4j.Logger

class Logging[F[_]: Effect[?[_]]](clazz: Class[_]) {
  private val logger: Logger = Logger.getLogger(this.getClass())

  def info(msg: String): F[Unit] = Effect[F].delay(logger.info(msg))

  def action[A](msg: String)(act: F[A]): F[A] =
    for {
      _ <- info(f"Starting ${msg}")
      a <- act
      _ <- info(f"Ending ${msg}")
    } yield (a)
}
