package mindmap

import cats.implicits._
import cats.effect.implicits._
import cats.effect.{ContextShift, Fiber, IO}
import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

object Test {
  def main(args: Array[String]): Unit = {
    val ex1 = Executors.newSingleThreadExecutor()
    val ex2 = Executors.newSingleThreadExecutor()
    val ecOne =
      ExecutionContext.fromExecutor(ex1)
    val ecTwo =
      ExecutionContext.fromExecutor(ex2)

    implicit val cs: ContextShift[IO] = IO.contextShift(ecOne)
    val csOne: ContextShift[IO] = IO.contextShift(ecOne)
    val csTwo: ContextShift[IO] = IO.contextShift(ecTwo)

    def infiniteIO(
      id: Int
    )(implicit cs: ContextShift[IO]): IO[Fiber[IO, Unit]] = {
      def repeat: IO[Unit] = IO(println(id)).flatMap(_ => IO.shift *> repeat)

      repeat.start
    }

    def doSteps(id: Int): IO[Unit] =
      IO {
        println(f"Starting step for ${id}")
      }.flatMap(_ =>
        IO.shift *> IO { println(f"Computing step for ${id}") }.flatMap(_ =>
          IO.shift *> IO { println(f"Ending step for ${id}") }
        )
      )

    val prog = (0 to 10).toList.map(i => doSteps(i)).parSequence

    prog.unsafeRunSync()
    ex1.shutdown()
    ex2.shutdown()
  }
}
