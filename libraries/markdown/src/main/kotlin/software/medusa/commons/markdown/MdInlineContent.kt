package software.medusa.commons.markdown

import org.commonmark.node.Node
import software.medusa.commons.markdown.CommonMark_utils.childNodes

@JvmInline
value class MdInlineContent(
    val inlineNodes: List<MdInlineNode>,
) {
  internal fun dump(): List<Node> = inlineNodes.map { it.dump() }

  companion object {
    internal fun load(
        parentNode: Node,
    ): MdInlineContent =
        MdInlineContent(
            inlineNodes = parentNode.childNodes.map { MdInlineNode.load(it) }.toList(),
        )
  }
}
