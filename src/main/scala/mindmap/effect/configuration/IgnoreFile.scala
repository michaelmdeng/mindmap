package mindmap.effect.configuration

import cats.Id
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.PathMatcher
import scala.io.Source

import mindmap.model.configuration.IgnoreAlgebra

class IgnoreFile extends IgnoreAlgebra[Id] {
  private val wikiDir = "/home/mdeng/Dropbox/vimwiki/wiki"
  private val ignoreFile = ".mindmapignore"
  private val fs = FileSystems.getDefault()
  private val markdownFiles =
    fs.getPathMatcher("glob:/home/mdeng/Dropbox/vimwiki/wiki/**.md")

  val ignores: List[String] =
    Source.fromFile(f"${wikiDir}/${ignoreFile}").getLines().toList

  private val pathMatchers: List[PathMatcher] = ignores.map(ignore => {
    fs.getPathMatcher(f"glob:${wikiDir}/${ignore}")
  })

  def isIgnoreFile(path: Path): Boolean = {
    val isMarkdown = markdownFiles.matches(path)
    val isIgnore = pathMatchers.foldLeft(false)((acc, matcher) => {
      acc || matcher.matches(path)
    })

    !isMarkdown || isIgnore
  }
}

object Ignores {
  private val fs = FileSystems.getDefault()
  private val diaryFiles =
    fs.getPathMatcher("glob:/home/mdeng/Dropbox/vimwiki/wiki/diary/**")
  private val markdownFiles =
    fs.getPathMatcher("glob:/home/mdeng/Dropbox/vimwiki/wiki/**.md")
  private val interviewFiles = fs.getPathMatcher(
    "glob:/home/mdeng/Dropbox/vimwiki/wiki/interview-fall-2019/**"
  )

  def isIgnoreFile(path: Path): Boolean = {
    markdownFiles.matches(path) && !diaryFiles.matches(path) && !interviewFiles
      .matches(path)
  }
}
