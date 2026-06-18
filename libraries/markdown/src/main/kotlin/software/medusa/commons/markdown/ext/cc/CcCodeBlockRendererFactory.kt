package software.medusa.commons.markdown.ext.cc

import org.commonmark.renderer.NodeRenderer
import org.commonmark.renderer.markdown.MarkdownNodeRendererContext
import org.commonmark.renderer.markdown.MarkdownNodeRendererFactory

internal object CcCodeBlockRendererFactory : MarkdownNodeRendererFactory {
  override fun create(context: MarkdownNodeRendererContext): NodeRenderer =
      CcCodeBlockRenderer(context)

  override fun getSpecialCharacters(): MutableSet<Char> = mutableSetOf()
}
