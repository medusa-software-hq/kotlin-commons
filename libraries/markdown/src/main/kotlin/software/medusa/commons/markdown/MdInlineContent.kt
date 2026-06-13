package software.medusa.commons.markdown

import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Link
import org.commonmark.node.Node
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text
import software.medusa.commons.markdown.CommonMark_utils.childNodes

@JvmInline
value class MdInlineContent(
    val inlineNodes: List<MdInlineNode>,
) {
  internal fun dump(): List<Node> = inlineNodes.map { it.dump() }

  companion object {
    fun load(
        parentNode: Node,
    ): MdInlineContent =
        MdInlineContent(
            inlineNodes =
                parentNode.childNodes
                    .map { parseInline(it) }
                    .filter { it != MdInlineNode.Text("") }
                    .toList(),
        )

    private fun parseInline(node: Node): MdInlineNode =
        when (node) {
          is Text -> MdInlineNode.Text(node.literal)
          is Code -> MdInlineNode.Code(node.literal)
          is Emphasis -> MdInlineNode.Emphasis(load(node).inlineNodes)
          is StrongEmphasis -> MdInlineNode.Strong(load(node).inlineNodes)
          is Link ->
              MdInlineNode.Link(
                  destination = node.destination,
                  title = node.title,
                  content = load(node).inlineNodes,
              )

          is SoftLineBreak -> MdInlineNode.SoftBreak
          is HardLineBreak -> MdInlineNode.HardBreak
          else -> throw MdParseException("Unsupported inline node: ${node::class.simpleName}")
        }
  }
}
