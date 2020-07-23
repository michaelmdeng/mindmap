package mindmap.effect.configuration

import cats.effect.Effect
import cats.implicits._
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.PathMatcher
import scala.io.Source

import mindmap.model.configuration.IgnoreAlgebra

class IgnoreFile[F[_]: Effect[?[_]]] extends IgnoreAlgebra[F] {
  private val wikiDir = "/home/mdeng/Dropbox/vimwiki/wiki"
  private val ignoreFile = ".mindmapignore"
  private val fs = FileSystems.getDefault()
  private val markdownFiles =
    fs.getPathMatcher("glob:/home/mdeng/Dropbox/vimwiki/wiki/**.md")

  val ignores: F[List[String]] = Effect[F].delay {
    Source.fromFile(f"${wikiDir}/${ignoreFile}").getLines().toList
  }

  private val pathMatchers: F[List[PathMatcher]] = for {
    is <- ignores
  } yield {
    is.map(ignore => fs.getPathMatcher(f"glob:${wikiDir}/${ignore}"))
  }

  def isIgnoreFile(path: Path): F[Boolean] =
    for {
      matchers <- pathMatchers
    } yield {
      val isMarkdown = markdownFiles.matches(path)
      val isIgnore = matchers.foldLeft(false)((acc, matcher) => {
        acc || matcher.matches(path)
      })

      !isMarkdown || isIgnore
    }
}
