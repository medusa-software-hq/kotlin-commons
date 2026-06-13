package software.medusa.commons.markdown.ext.cc

import org.commonmark.Extension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import software.medusa.commons.markdown.ext.cc.internal.CcCodeBlockParser
import software.medusa.commons.markdown.ext.cc.internal.CcHtmlNodeRenderer
import software.medusa.commons.markdown.ext.cc.internal.CcInlineCodeParser

internal class CcExtension private constructor() :
    Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension {
  companion object {
    fun create(): Extension = CcExtension()
  }

  override fun extend(parserBuilder: Parser.Builder) {
    parserBuilder.customBlockParserFactory(CcCodeBlockParser.Factory())
    parserBuilder.customInlineContentParserFactory(CcInlineCodeParser.Factory())
  }

  override fun extend(rendererBuilder: HtmlRenderer.Builder) {
    rendererBuilder.nodeRendererFactory(::CcHtmlNodeRenderer)
  }
}
