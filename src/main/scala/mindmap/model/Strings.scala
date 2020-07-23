package mindmap.model

object Strings {
  def stripPrefix(str: String, f: Char => Boolean): String =
    str.dropWhile(f)

  def stripSuffix(str: String, f: Char => Boolean): String =
    str.reverse.dropWhile(f).reverse

  def stripPrecedingWhitespace(str: String): String =
    stripPrefix(str, (c: Char) => c == ' ' || c == '\t')

  def stripTrailingWhitespace(str: String): String =
    stripSuffix(str, (c: Char) => c == ' ' || c == '\t')

  def stripPrecedingLineBreaks(str: String): String =
    stripPrefix(str, (c: Char) => c == '\n')

  def stripTrailingLineBreaks(str: String): String =
    stripSuffix(str, (c: Char) => c == '\n')

  implicit class MindmapStrings(s: String) {
    def stripPrefix(f: Char => Boolean): String = Strings.stripPrefix(s, f)

    def stripSuffix(f: Char => Boolean): String = Strings.stripSuffix(s, f)

    def stripPrecedingWhitespace: String = Strings.stripPrecedingWhitespace(s)

    def stripTrailingWhitespace: String = Strings.stripTrailingWhitespace(s)

    def stripPrecedingLineBreaks: String = Strings.stripPrecedingLineBreaks(s)

    def stripTrailingLineBreaks: String = Strings.stripTrailingLineBreaks(s)
  }
}
