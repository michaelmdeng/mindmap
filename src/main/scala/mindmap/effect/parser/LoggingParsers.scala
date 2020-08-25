package mindmap.effect.parser

import cats.effect.Effect
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import scala.util.parsing.input.CharSequenceReader
import scala.util.parsing.input.Reader

import mindmap.effect.Logging
import mindmap.model.parser.SharedParsers

trait LoggingParsers extends SharedParsers {
  def parseAndLog[F[_]: Effect[?[_]]: Logging[?[_]], T](
    p: Parser[T],
    in: CharSequence,
    name: String
  ): F[ParseResult[T]] = parseAndLog(p, new CharSequenceReader(in), name)

  def parseAndLog[F[_]: Effect[?[_]]: Logging[?[_]], T](
    p: Parser[T],
    in: Reader[Char],
    name: String
  ): F[ParseResult[T]] = {
    for {
      _ <- Logging(this.getClass()).debug(f"Trying parser: ${name} at ${in}")
      result <- parse(p, in).pure[F]
      _ <- result match {
        case Success(r, _) => {
          Logging(this.getClass())
            .debug(f"Parsed result for parser: ${name} --> ${r}")
        }
        case _: NoSuccess => Effect[F].unit
      }
    } yield (result)
  }
}
