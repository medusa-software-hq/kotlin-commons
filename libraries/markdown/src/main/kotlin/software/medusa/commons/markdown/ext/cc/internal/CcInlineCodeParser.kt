package software.medusa.commons.markdown.ext.cc.internal

import org.commonmark.node.Text
import org.commonmark.parser.beta.InlineContentParser
import org.commonmark.parser.beta.InlineContentParserFactory
import org.commonmark.parser.beta.InlineParserState
import org.commonmark.parser.beta.ParsedInline
import software.medusa.commons.markdown.ext.cc.CcInlineCode

internal class CcInlineCodeParser : InlineContentParser {
  override fun tryParse(inlineParserState: InlineParserState): ParsedInline {
    val scanner = inlineParserState.scanner()
    scanner.next()
    val afterOpening = scanner.position()

    if (scanner.find(CLOSE) == -1) {
      return ParsedInline.of(Text(OPEN.toString()), afterOpening)
    }

    val content = scanner.getSource(afterOpening, scanner.position()).content
    scanner.next()
    return ParsedInline.of(CcInlineCode(content), scanner.position())
  }

  internal class Factory : InlineContentParserFactory {
    override fun getTriggerCharacters(): Set<Char> = setOf(OPEN)

    override fun create(): InlineContentParser = CcInlineCodeParser()
  }

  companion object {
    private const val OPEN = ''
    private const val CLOSE = ''
  }
}
