package software.medusa.commons.markdown.ext.cc.internal

import org.commonmark.node.Node
import org.commonmark.renderer.NodeRenderer
import org.commonmark.renderer.html.HtmlNodeRendererContext
import software.medusa.commons.markdown.ext.cc.CcCodeBlock
import software.medusa.commons.markdown.ext.cc.CcInlineCode

internal class CcHtmlNodeRenderer(
    private val context: HtmlNodeRendererContext,
) : NodeRenderer {
  private val html = context.writer

  override fun getNodeTypes(): Set<Class<out Node>> =
      setOf(CcCodeBlock::class.java, CcInlineCode::class.java)

  override fun render(node: Node) {
    when (node) {
      is CcCodeBlock -> {
        html.line()
        html.tag("pre", context.extendAttributes(node, "pre", emptyMap()))
        html.tag("code", context.extendAttributes(node, "code", emptyMap()))
        html.text(node.literal)
        html.tag("/code")
        html.tag("/pre")
        html.line()
      }
      is CcInlineCode -> {
        html.tag("code", context.extendAttributes(node, "code", emptyMap()))
        html.text(node.literal)
        html.tag("/code")
      }
    }
  }
}
