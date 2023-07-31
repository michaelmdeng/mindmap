package mindmap.model.parser.markdown

import org.scalatest.funspec.AnyFunSpec

import mindmap.model.Tag

class BlockParsersSpec extends AnyFunSpec {
  val parsers = new BlockParsers()

  def assertParsed[T](result: parsers.ParseResult[T], f: T => Boolean): Unit = {
    result match {
      case parsers.Success(v, _MarkdownParser) =>
        assert(f(v), f"Parsed value ${v} did not satisfy predicate")
      case parsers.Error(e, _) => assert(false, f"Parsing errored due to: ${e}")
      case parsers.Failure(e, _) =>
        assert(false, f"Parsing failed due to: ${e}")
    }
  }

  def assertParsed[T](result: parsers.ParseResult[T], value: T): Unit = {
    assertParsed(result, (t: T) => t == value)
  }

  describe("tags") {
    val parser = parsers.tags
    val tags = TagBlock(List(Tag("foo"), Tag("bar")))
    val tagsWithSpaces = TagBlock(tags.tags ++ List(Tag("tag with space")))
    val tagLine = ":" + tags.tags.map(_.name).mkString(":") + ":"
    val tagLineWithSpaces =
      ":" + tagsWithSpaces.tags.map(_.name).mkString(":") + ":"

    it("should parse all tags") {
      val result = parsers.parse(parser, tagLine)
      assertParsed(
        result,
        (block: TagBlock) => {
          block.tags.zip(tags.tags).forall { case (a, b) =>
            a.name == b.name
          }
        }
      )
    }

    it("should allow tags with spaces") {
      val result = parsers.parse(parser, tagLineWithSpaces)
      assertParsed(
        result,
        (block: TagBlock) => {
          block.tags.zip(tags.tags).forall { case (a, b) =>
            a.name == b.name
          }
        }
      )
    }

    it("should allow preceding indentation up to 3") {
      val result = parsers.parse(parser, f"   ${tagLine}")
      assertParsed(
        result,
        (block: TagBlock) => {
          block.tags.zip(tags.tags).forall { case (a, b) =>
            a.name == b.name
          }
        }
      )
    }

    it("should strip trailing whitespace") {
      val result = parsers.parse(parser, f"${tagLine}  \t  ")
      assertParsed(
        result,
        (block: TagBlock) => {
          block.tags.zip(tags.tags).forall { case (a, b) =>
            a.name == b.name
          }
        }
      )
    }
  }

  describe("headers") {
    val parser = parsers.header
    val headerString = "my header"
    val headerStringWithSpace = "my  \t  header"

    it("should parse headers") {
      val result = parsers.parse(parser, f"# ${headerString}")
      assertParsed(result, Header(headerString, 1))
    }

    it("should allow indentation up to 3 spaces") {
      val result = parsers.parse(parser, f"   # ${headerString}")
      assertParsed(result, Header(headerString, 1))
    }

    it("should ignore leading whitespace") {
      val result = parsers.parse(parser, f"# \t   ${headerString}")
      assert(result.get == Header(headerString, 1))
    }

    it("should ignore trailing whitespace") {
      val result = parsers.parse(parser, f"# ${headerString}    \t  ")
      assert(result.get == Header(headerString, 1))
    }

    it("should prerserve interior whitespace") {
      val result = parsers.parse(parser, f"## ${headerStringWithSpace} ###")
      assert(result.get == Header(headerStringWithSpace, 2))
    }

    it("should have a maximum level of 6") {
      val result = parsers.parse(parser, f"################# ${headerString}")
      assert(result.get == Header(headerString, 6))
    }

    it("should parse closing headers") {
      val result = parsers.parse(parser, f"# ${headerString} #")
      assert(result.get == Header(headerString, 1))
    }

    it("should determine level from opening #") {
      val result = parsers.parse(parser, f"## ${headerString} ###")
      assert(result.get == Header(headerString, 2))
    }
  }

  describe("blockquotes") {
    val parser = parsers.blockQuote
    val blockQuote = BlockQuote("foo\nbar")
    val quote = "> foo\n> bar"
    val quoteWithLeadingWhitespace = ">   \t foo\n>  bar"
    val quoteWithTrailingWhitespace = "> foo   \t \n> bar  "

    it("should parse the quote text") {
      val result = parsers.parse(parser, quote)
      assertParsed(result, blockQuote)
    }

    it("should ignore leading whitespace") {
      val result = parsers.parse(parser, quoteWithLeadingWhitespace)
      assertParsed(result, blockQuote)
    }

    it("should ignore trailing whitespace") {
      val result = parsers.parse(parser, quoteWithTrailingWhitespace)
      assertParsed(result, blockQuote)
    }
  }

  describe("code block") {
    val language = "scala"
    val languageWithWhitespace = "scala\t   andmore"

    val code = """\tdef foo: String = "bar"\n\tdef bar: Int = 0"""
    val block = f"""```${language}
${code}
```
"""

    val blockWithIndentation = f"""   ```${language}
${code}
```
"""

    val blockNoLanguage = f"""```
${code}
```
"""

    val blockLanguageWithWhiteSpace = f"""```${languageWithWhitespace}
${code}
```
"""

    val blockWithLeadingTrailingWhitespace = f"""``` \t  ${language}   \t
${code}
```
"""

    val parser = parsers.codeBlock

    it("should parse language and code") {
      val result = parsers.parse(parser, block)
      assertParsed(
        result,
        (b: CodeBlock) => b.language.get == language && b.code == code
      )
    }

    it("should parse code only if no language specified") {
      val result = parsers.parse(parser, blockNoLanguage)
      assertParsed(result, (b: CodeBlock) => b.language.isEmpty)
    }

    it("should allow leading indentation up to 3 spaces") {
      val result = parsers.parse(parser, blockWithIndentation)
      assertParsed(
        result,
        (b: CodeBlock) => b.language.get == language && b.code == code
      )
    }

    it("should strip leading and trailing whitespace") {
      val result = parsers.parse(parser, blockWithLeadingTrailingWhitespace)
      assertParsed(result, (b: CodeBlock) => b.language.get == language)
    }

    it("should preserve whitespace in info") {
      val result = parsers.parse(parser, blockLanguageWithWhiteSpace)
      assertParsed(
        result,
        (b: CodeBlock) => b.language.get == languageWithWhitespace
      )
    }
  }
}
