package mindmap.model

/** Unresolved Zettelkasten, consisting of the [[Note]] and [[Tag]] objects
  * without links resolved.
  */
case class Repository(
  noteTags: Map[Note, Set[Tag]],
  noteLinks: Map[Note, List[UnresolvedLink]]
)
