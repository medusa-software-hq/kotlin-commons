package software.medusa.commons.markdown.ext.cc.internal

import org.commonmark.node.Block
import org.commonmark.parser.SourceLine
import org.commonmark.parser.block.AbstractBlockParser
import org.commonmark.parser.block.AbstractBlockParserFactory
import org.commonmark.parser.block.BlockContinue
import org.commonmark.parser.block.BlockStart
import org.commonmark.parser.block.MatchedBlockParser
import org.commonmark.parser.block.ParserState
import org.commonmark.text.Characters
import software.medusa.commons.markdown.ext.cc.CcCodeBlock

internal class CcCodeBlockParser
private constructor(
    private val openingIndent: Int,
) : AbstractBlockParser() {

  private val block = CcCodeBlock()
  private val literal = StringBuilder()
  private var openingLineConsumed = false

  override fun getBlock(): Block = block

  override fun tryContinue(state: ParserState): BlockContinue {
    val nextNonSpace = state.nextNonSpaceIndex
    var newIndex = state.index
    val line = state.line.content
    if (
        state.indent < CODE_BLOCK_INDENT &&
            nextNonSpace < line.length &&
            tryClosing(line, nextNonSpace)
    ) {
      return BlockContinue.finished()
    }
    var i = openingIndent
    while (i > 0 && newIndex < line.length && line[newIndex] == ' ') {
      newIndex++
      i--
    }
    return BlockContinue.atIndex(newIndex)
  }

  override fun addLine(line: SourceLine) {
    if (!openingLineConsumed) {
      openingLineConsumed = true
      return
    }
    literal.append(line.content)
    literal.append('\n')
  }

  override fun closeBlock() {
    block.literal = literal.toString()
  }

  private fun tryClosing(line: CharSequence, index: Int): Boolean {
    if (index >= line.length || line[index] != CLOSE) return false
    val after = Characters.skipSpaceTab(line, index + 1, line.length)
    return after == line.length
  }

  internal class Factory : AbstractBlockParserFactory() {
    override fun tryStart(state: ParserState, matchedBlockParser: MatchedBlockParser): BlockStart? {
      if (state.indent >= CODE_BLOCK_INDENT) return BlockStart.none()
      val nextNonSpace = state.nextNonSpaceIndex
      if (isOpening(state.line.content, nextNonSpace)) {
        return BlockStart.of(CcCodeBlockParser(state.indent)).atIndex(nextNonSpace + 1)
      }
      return BlockStart.none()
    }
  }

  companion object {
    private const val CODE_BLOCK_INDENT = 4
    private const val OPEN = '\u0002'
    private const val CLOSE = '\u0003'

    private fun isOpening(line: CharSequence, index: Int): Boolean =
        index < line.length && line[index] == OPEN && index + 1 == line.length
  }
}
