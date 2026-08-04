package software.medusa.commons.markdown

import org.commonmark.node.Code as CmCode
import org.commonmark.node.Emphasis as CmEmphasis
import org.commonmark.node.HardLineBreak
import org.commonmark.node.HtmlInline as CmHtmlInline
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
          is CmHtmlInline -> Html.load(htmlInline = node)

          // Any inline node type we don't otherwise model (e.g. images, or node types
          // introduced by future CommonMark/extension versions) degrades to its plain text
          // content instead of failing the whole parse.
          else -> literalTextOf(node)?.let { Text(it) }
        }

    /** The concatenated literal text of [node]'s descendant text nodes, without mutating it. */
    private fun literalTextOf(node: Node): String? =
        buildString {
              generateSequence(node.firstChild) { it.next }
                  .forEach { child ->
                    if (child is CmText) append(child.literal)
                    else literalTextOf(child)?.let(::append)
                  }
            }
            .takeIf { it.isNotEmpty() }
  }

  internal abstract fun dump(): Node

  fun render(): String = MdDocument.render(inlineNode = this)

  /**
   * This node and all inline nodes nested within it, in document order. Leaf nodes yield just
   * themselves; container nodes (emphasis, strong, link) yield themselves followed by their
   * descendants.
   */
  open fun visitInlineNodes(): Sequence<MdInlineNode> = sequenceOf(this)

  data class Text(
      val text: String,
  ) : MdInlineNode() {
    companion object {
      fun load(
          textNode: CmText,
      ): MdInlineNode.Text? =
          if (textNode.literal.isNotEmpty()) {
            Text(textNode.literal)
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

  /** Raw inline HTML (e.g. `<br>`), kept verbatim rather than interpreted. */
  data class Html(
      val literal: String,
  ) : MdInlineNode() {
    companion object {
      fun load(
          htmlInline: CmHtmlInline,
      ): MdInlineNode.Html? =
          if (htmlInline.literal.isNotEmpty()) {
            Html(htmlInline.literal)
          } else {
            null
          }
    }

    override fun dump(): Node = CmHtmlInline().also { it.literal = literal }
  }

  data class Emphasis(
      val content: List<MdInlineNode>,
  ) : MdInlineNode() {
    companion object {
      fun of(
          text: String,
      ): MdInlineNode.Emphasis =
          MdInlineNode.Emphasis(
              content =
                  listOf(
                      Text(text),
                  ),
          )
    }

    override fun dump(): Node =
        CmEmphasis().also { emphasis ->
          content.forEach { inline -> emphasis.appendChild(inline.dump()) }
        }

    override fun visitInlineNodes(): Sequence<MdInlineNode> =
        sequenceOf(this) + content.asSequence().flatMap { it.visitInlineNodes() }
  }

  data class Strong(
      val content: List<MdInlineNode>,
  ) : MdInlineNode() {
    companion object {
      fun of(
          text: String,
      ): MdInlineNode.Strong =
          MdInlineNode.Strong(
              content =
                  listOf(
                      Text(text),
                  ),
          )
    }

    override fun dump(): Node =
        StrongEmphasis().also { strong ->
          content.forEach { inline -> strong.appendChild(inline.dump()) }
        }

    override fun visitInlineNodes(): Sequence<MdInlineNode> =
        sequenceOf(this) + content.asSequence().flatMap { it.visitInlineNodes() }
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

    override fun visitInlineNodes(): Sequence<MdInlineNode> =
        sequenceOf(this) + content.asSequence().flatMap { it.visitInlineNodes() }
  }

  data object SoftBreak : MdInlineNode() {
    override fun dump(): Node = SoftLineBreak()
  }

  data object HardBreak : MdInlineNode() {
    override fun dump(): Node = HardLineBreak()
  }
}
