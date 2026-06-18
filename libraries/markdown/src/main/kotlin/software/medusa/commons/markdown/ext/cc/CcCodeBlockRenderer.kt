package software.medusa.commons.markdown.ext.cc

import org.commonmark.node.Node
import org.commonmark.renderer.NodeRenderer
import org.commonmark.renderer.markdown.MarkdownNodeRendererContext
import org.commonmark.renderer.markdown.MarkdownWriter
import software.medusa.commons.markdown.ControlChar

internal class CcCodeBlockRenderer(
    private val context: MarkdownNodeRendererContext,
) : NodeRenderer {
  private val writer: MarkdownWriter = context.writer

  override fun getNodeTypes(): Set<Class<out Node>> = setOf(CcCodeBlock::class.java)

  override fun render(node: Node) {
    val ccCodeBlock =
        node as? CcCodeBlock ?: error("Unexpected node type: ${node::class.simpleName}")
    val lines = ccCodeBlock.literal.split("\n").dropLastWhile(String::isEmpty)

    writer.raw(ControlChar.STX.toString())
    writer.line()
    lines.forEach { line ->
      writer.raw(line)
      writer.line()
    }
    writer.raw(ControlChar.ETX.toString())
    writer.block()
  }
}
