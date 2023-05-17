package mindmap.model.configuration

import org.scalatest.funspec.AnyFunSpec

class GrapherArgsSpec extends AnyFunSpec {
  describe("GrapherArgs") {
    it("should parse paths") {
      val args =
        List(
          "--collection-path",
          "/home/mdeng/MyDrive/vimwiki",
          "--network-path",
          "public/graph"
        )
      val grapherArgs = GrapherArgs(args)
      assert(grapherArgs.collectionPath() == "/home/mdeng/MyDrive/vimwiki")
      assert(grapherArgs.networkPath() == "public/graph")
    }
  }
}
