package mindmap.model.configuration

import org.scalatest.funspec.AnyFunSpec

class ServerArgsSpec extends AnyFunSpec {
  describe("ServerArgs") {
    it("should parse path") {
      val args =
        List("--path", "/home/mdeng/MyDrive/vimwiki")
      val serverArgs = ServerArgs(args)
      assert(serverArgs.path() == "/home/mdeng/MyDrive/vimwiki")
    }
  }
}
