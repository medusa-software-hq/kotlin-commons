package software.medusa.commons.markdown

import org.commonmark.node.Block
import org.commonmark.node.Document
import org.commonmark.node.Heading
import org.commonmark.node.Paragraph
import org.commonmark.parser.Parser
import org.commonmark.renderer.markdown.MarkdownRenderer
import software.medusa.commons.markdown.ext.cc.CcCodeBlockRendererFactory
import software.medusa.commons.markdown.ext.cc.CcExtension
import software.medusa.commons.markdown.utils.PoppableQueue
import software.medusa.commons.markdown.utils.poppableQueueOf

@JvmInline
value class MdDocument(
    val rootChapter: MdChapter,
) {
  companion object {
    private val commonmarkParser = Parser.builder().extensions(listOf(CcExtension.create())).build()

    private val commonmarkMarkdownRenderer =
        MarkdownRenderer.builder().nodeRendererFactory(CcCodeBlockRendererFactory).build()

    fun parse(markdownSource: String): MdDocument {
      val document = commonmarkParser.parse(markdownSource)
      val blocks =
          generateSequence(document.firstChild) { it.next }
              .map {
                it as? Block
                    ?: error("Unexpected non-block top-level node: ${it::class.simpleName}")
              }
              .toList()
      return load(poppableQueueOf(blocks))
    }

    internal fun load(
        blockQueue: PoppableQueue<Block>,
    ): MdDocument {
      val firstBlock =
          blockQueue.pop() ?: throw MdParseException("Empty documents are not supported")

      val rootHeading =
          firstBlock as? Heading
              ?: throw MdParseException("Expected ATX heading: ${firstBlock::class.simpleName}")

      val rootChapter =
          MdChapter.load(
              blockQueue = blockQueue,
              leadHeading = rootHeading,
          )

      return MdDocument(rootChapter = rootChapter)
    }

    internal fun render(
        inlineNode: MdInlineNode,
    ): String =
        commonmarkMarkdownRenderer
            .render(
                Paragraph().apply { appendChild(inlineNode.dump()) },
            )
            .trimEnd('\n')
  }

  fun render(): String = commonmarkMarkdownRenderer.render(dump())

  private fun dump(): Document =
      Document().also { document ->
        rootChapter.dump(level = 1).forEach(document::appendChild)
      }
}
