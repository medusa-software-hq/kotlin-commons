package software.medusa.commons.markdown

import org.commonmark.node.Block
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.CustomBlock
import org.commonmark.node.Document
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.Heading
import org.commonmark.node.HtmlBlock
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.LinkReferenceDefinition
import org.commonmark.node.ListBlock as CmListBlock
import org.commonmark.node.ListItem
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph as CmParagraph
import org.commonmark.node.ThematicBreak
import software.medusa.commons.markdown.CommonMark_utils.childNodes
import software.medusa.commons.markdown.ext.cc.CcCodeBlock

sealed class MdBlock {
  data class Paragraph(
      val content: MdInlineContent,
  ) : MdBlock() {
    override fun dump(): CmParagraph =
        CmParagraph().also { paragraph ->
          content.dump().forEach(paragraph::appendChild)
        }

    companion object {
      internal fun load(
          paragraph: CmParagraph,
      ): MdBlock =
          Paragraph(
              content = MdInlineContent.load(parentNode = paragraph),
          )
    }
  }

  data class ListBlock(
      val topLevel: Level,
  ) : MdBlock() {
    override fun dump(): BulletList =
        topLevel.dump()

    @JvmInline
    value class Level(
        val items: List<Item>,
    ) {
      companion object {
        internal fun load(
            listBlock: CmListBlock,
        ): Level =
            Level(
                items =
                    listBlock.childNodes
                        .map { node ->
                          val listItem =
                              node as? ListItem
                                  ?: throw MdParseException(
                                      "Expected list item, got ${node::class.simpleName}",
                                  )

                          Item.load(listItem = listItem)
                        }
                        .toList(),
            )
      }

      init {
        require(items.isNotEmpty()) { "List levels must have at least one item" }
      }

      internal fun dump(): BulletList =
          BulletList().also { list ->
            list.marker = "-"
            items.forEach { item -> list.appendChild(item.dump()) }
          }
    }

    data class Item(
        val content: MdInlineContent,
        val nestedLevel: Level?,
    ) {
      internal fun dump(): ListItem =
          ListItem().also { item ->
            item.appendChild(
                CmParagraph().also { paragraph ->
                  content.dump().forEach(paragraph::appendChild)
                },
            )
            nestedLevel?.let {
              item.appendChild(it.dump())
            }
          }

      companion object {
        internal fun load(
            listItem: ListItem,
        ): Item {
          val childNodes = listItem.childNodes.toList()

          val paragraph =
              childNodes.firstOrNull() as? CmParagraph
                  ?: throw MdParseException(
                      "Expected list item to start with a paragraph",
                  )

          val body = MdInlineContent.load(parentNode = paragraph)

          if (childNodes.size == 1) return Item(content = body, nestedLevel = null)

          require(childNodes.size == 2) {
            "List item has ${childNodes.size} child nodes; expected 1 or 2"
          }

          val nestedListBlock =
              childNodes[1] as? CmListBlock
                  ?: throw MdParseException(
                      "Expected second child of list item to be a list block, " +
                          "got ${childNodes[1]::class.simpleName}",
                  )

          return Item(
              content = body,
              nestedLevel = Level.load(listBlock = nestedListBlock),
          )
        }
      }
    }

    companion object {
      internal fun load(
          listBlock: CmListBlock,
      ): MdBlock.ListBlock =
          ListBlock(
              topLevel =
                  Level.load(
                      listBlock = listBlock,
                  ),
          )
    }
  }

  data class CodeBlock(
      val info: String? = null,
      val code: String,
  ) : MdBlock() {
    companion object {
      internal fun load(
          fencedCodeBlock: FencedCodeBlock,
      ): CodeBlock =
          CodeBlock(
              info = fencedCodeBlock.info.takeUnless { it.isNullOrEmpty() },
              code = fencedCodeBlock.literal,
          )
    }

    override fun dump(): FencedCodeBlock =
        FencedCodeBlock().also { codeBlock ->
          codeBlock.literal = code
          codeBlock.info = info
        }
  }

  data class RawCodeBlock(
      val code: String,
  ) : MdBlock() {
    companion object {
      internal fun load(
          ccCodeBlock: CcCodeBlock,
      ): RawCodeBlock =
          RawCodeBlock(
              code = ccCodeBlock.literal,
          )
    }

    override fun dump(): CcCodeBlock = CcCodeBlock().also { codeBlock -> codeBlock.literal = code }
  }

  companion object {
    internal fun load(
        block: Block,
    ): MdBlock =
        when (block) {
          is CmParagraph -> Paragraph.load(paragraph = block)

          is CmListBlock -> MdBlock.ListBlock.load(listBlock = block)

          is FencedCodeBlock -> CodeBlock.load(fencedCodeBlock = block)

          is CustomBlock ->
              when (block) {
                is CcCodeBlock -> RawCodeBlock.load(ccCodeBlock = block)

                // We don't install third-party extensions
                else ->
                    throw IllegalStateException(
                        "Unrecognized custom block: ${block::class.simpleName}"
                    )
              }

          is BlockQuote ->
              throw MdParseException("Unsupported top-level node: ${block::class.simpleName}")

          is ThematicBreak ->
              throw MdParseException("Unsupported top-level node: ${block::class.simpleName}")

          is IndentedCodeBlock ->
              throw MdParseException("Unsupported top-level node: ${block::class.simpleName}")

          is HtmlBlock ->
              throw MdParseException("Unsupported top-level node: ${block::class.simpleName}")

          is LinkReferenceDefinition ->
              throw MdParseException("Unsupported top-level node: ${block::class.simpleName}")

          // Document should be the top-level node
          is Document -> throw IllegalStateException("Document node is not expected here")

          // Headings should be recognized as chapter-starting nodes, not ordinary blocks
          is Heading -> throw IllegalStateException("Heading node is not expected here")

          // List items should occur only within list blocks
          is ListItem -> throw IllegalStateException("List item node is not expected here")

          else -> throw IllegalStateException("Unrecognized block: ${block::class.simpleName}")
        }
  }

  internal abstract fun dump(): Block
}

private fun configureList(list: CmListBlock) {
  list.isTight = false

  when (list) {
    is BulletList -> list.marker = "-"

    is OrderedList -> {
      list.markerStartNumber = 1
      list.markerDelimiter = "."
    }
  }
}
