package mindmap.model.configuration

import org.rogach.scallop._

class ServerArgs(args: List[String]) extends ScallopConf(args) {
  val path =
    opt[String](required = true, default = Some(ServerArgs.DEFAULT_PATH))
  verify()
}

object ServerArgs {
  private final val DEFAULT_PATH = "/home/mdeng/MyDrive/vimwiki"

  def apply(args: List[String]): ServerArgs = new ServerArgs(
    args
  )
}
