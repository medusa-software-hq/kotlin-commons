package software.medusa.commons.markdown

import org.commonmark.node.Code as CmCode
import org.commonmark.node.Emphasis as CmEmphasis
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Link as CmLink
import org.commonmark.node.Node
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text as CmText

sealed class MdInlineNode {
  companion object {
    internal fun load(node: Node): MdInlineNode? =
        when (node) {
          is CmText -> Text.load(textNode = node)
          is CmCode -> Code(node.literal)
          is CmEmphasis -> Emphasis(MdInlineContent.load(node).inlineNodes)
          is StrongEmphasis -> Strong(MdInlineContent.load(node).inlineNodes)
          is CmLink ->
              Link(
                  destination = node.destination,
                  title = node.title,
                  content = MdInlineContent.load(node).inlineNodes,
              )

          is SoftLineBreak -> SoftBreak
          is HardLineBreak -> HardBreak
          else -> throw MdParseException("Unsupported inline node: ${node::class.simpleName}")
        }
  }

  internal abstract fun dump(): Node

  fun render(): String = MdDocument.render(inlineNode = this)

  data class Text(
      val text: String,
  ) : MdInlineNode() {
    companion object {
      fun load(
          textNode: CmText,
      ): MdInlineNode.Text? =
          if (textNode.literal.isNotEmpty()) {
            MdInlineNode.Text(textNode.literal)
          } else {
            null
          }
    }

    init {
      require(text.isNotEmpty()) { "Text node cannot be empty" }
    }

    override fun dump(): Node = CmText(text)
  }

  data class Code(
      val code: String,
  ) : MdInlineNode() {
    override fun dump(): Node = CmCode(code)
  }

  data class Emphasis(
      val content: List<MdInlineNode>,
  ) : MdInlineNode() {
    override fun dump(): Node =
        CmEmphasis().also { emphasis ->
          content.forEach { inline -> emphasis.appendChild(inline.dump()) }
        }
  }

  data class Strong(
      val content: List<MdInlineNode>,
  ) : MdInlineNode() {
    override fun dump(): Node =
        StrongEmphasis().also { strong ->
          content.forEach { inline -> strong.appendChild(inline.dump()) }
        }
  }

  data class Link(
      val destination: String,
      val title: String?,
      val content: List<MdInlineNode>,
  ) : MdInlineNode() {
    override fun dump(): Node =
        CmLink(destination, title).also { link ->
          content.forEach { inline -> link.appendChild(inline.dump()) }
        }
  }

  data object SoftBreak : MdInlineNode() {
    override fun dump(): Node = SoftLineBreak()
  }

  data object HardBreak : MdInlineNode() {
    override fun dump(): Node = HardLineBreak()
  }
}
