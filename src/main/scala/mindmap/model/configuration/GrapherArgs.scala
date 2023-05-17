package mindmap.model.configuration

import org.rogach.scallop._

class GrapherArgs(args: List[String]) extends ScallopConf(args) {
  val collectionPath = opt[String](
    required = true,
    default = Some(GrapherArgs.DEFAULT_COLLECTION_PATH)
  )
  val networkPath =
    opt[String](
      required = true,
      default = Some(GrapherArgs.DEFAULT_NETWORK_PATH)
    )

  verify()
}

object GrapherArgs {
  private final val DEFAULT_COLLECTION_PATH = "/home/mdeng/MyDrive/vimwiki"
  private final val DEFAULT_NETWORK_PATH = "public/graph"

  def apply(args: List[String]): GrapherArgs = new GrapherArgs(
    args
  )
}
