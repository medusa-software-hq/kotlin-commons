package software.medusa.commons.markdown

import org.commonmark.node.Block

@JvmInline
value class MdElement(
    val blocks: List<MdBlock>,
) {
  internal fun dump(): List<Block> = blocks.map { it.dump() }

  companion object {
    val Empty = MdElement(emptyList())
  }
}
