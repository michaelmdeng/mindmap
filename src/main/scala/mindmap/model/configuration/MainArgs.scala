package mindmap.model.configuration

import org.rogach.scallop._

class MainArgs(args: List[String]) extends ScallopConf(args) {
  val clazz = opt[String](
    name = "class",
    required = true,
    validate = (s) => {
      s == "mindmap.Server" || s == "mindmap.Grapher"
    }
  )

  val remaining = trailArg[List[String]]()

  verify()
}

object MainArgs {
  def apply(args: List[String]): MainArgs = new MainArgs(
    args
  )
}
