package mindmap.model.configuration

import org.scalatest.funspec.AnyFunSpec

class MainArgsSpec extends AnyFunSpec {
  describe("MainArgs") {
    it("should parse Server") {
      val args =
        List(
          "--class",
          "mindmap.Server",
          "--",
          "--path",
          "/home/mdeng/MyDrive/vimwiki"
        )
      val mainArgs = MainArgs(args)
      assert(mainArgs.clazz() == "mindmap.Server")
      assert(
        mainArgs.remaining() == List("--path", "/home/mdeng/MyDrive/vimwiki")
      )
    }

    it("should parse Grapher") {
      val args =
        List(
          "--class",
          "mindmap.Grapher",
          "--",
          "--collection-path",
          "/home/mdeng/MyDrive/vimwiki",
          "--network-path",
          "public/graph"
        )
      val mainArgs = MainArgs(args)
      assert(mainArgs.clazz() == "mindmap.Grapher")
      assert(
        mainArgs.remaining() == List(
          "--collection-path",
          "/home/mdeng/MyDrive/vimwiki",
          "--network-path",
          "public/graph"
        )
      )
    }
  }
}
