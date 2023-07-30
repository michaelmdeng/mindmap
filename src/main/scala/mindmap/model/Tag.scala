package mindmap.model

import scala.math
import scala.util.Random

case class Tag(
  id: Long,
  name: String
) extends Entity

object Tag {
  def apply(name: String): Tag = Tag(math.abs(Random.nextLong()), name)
}
