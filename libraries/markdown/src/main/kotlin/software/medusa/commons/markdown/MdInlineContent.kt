package software.medusa.commons.markdown

import org.commonmark.node.Node
import software.medusa.commons.markdown.CommonMark_utils.childNodes

@JvmInline
value class MdInlineContent(
    val inlineNodes: List<MdInlineNode>,
) {
  internal fun dump(): List<Node> = inlineNodes.map { it.dump() }

  companion object {
    /** Wraps [text] as inline content consisting of a single text node. */
    fun of(
        text: String,
    ): MdInlineContent = MdInlineContent(inlineNodes = listOf(MdInlineNode.Text(text)))

    internal fun load(
        parentNode: Node,
    ): MdInlineContent =
        MdInlineContent(
            inlineNodes = parentNode.childNodes.mapNotNull { MdInlineNode.load(it) }.toList(),
        )
  }
}
