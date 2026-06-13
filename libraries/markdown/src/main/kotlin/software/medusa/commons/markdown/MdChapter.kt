package software.medusa.commons.markdown

import org.commonmark.node.Block
import org.commonmark.node.Heading
import org.commonmark.node.Node
import software.medusa.commons.markdown.utils.PoppableQueue
import software.medusa.commons.markdown.utils.popOfWhileNotNull

data class MdChapter(
    val title: MdInlineContent,
    val blocks: List<MdBlock>,
    val subChapters: List<MdChapter>,
) {
  internal fun dump(level: Int): List<Node> = buildList {
    add(
        Heading().also { heading ->
          heading.level = level
          title.dump().forEach(heading::appendChild)
        },
    )
    addAll(blocks.map { block -> block.dump() })
    subChapters.forEach { subChapter -> addAll(subChapter.dump(level = level + 1)) }
  }

  companion object {
    internal fun load(
        blockQueue: PoppableQueue<Block>,
        leadHeading: Heading,
    ): MdChapter {
      val leadHeadingLevel = leadHeading.level
      val title = MdInlineContent.load(parentNode = leadHeading)

      val leadingBlocks =
          blockQueue
              .popOfWhileNotNull { block ->
                // Stop processing top-level blocks when we encounter a heading
                block.takeUnless { it is Heading }
              }
              .map { block -> MdBlock.load(block = block) }
              .toList()

      if (blockQueue.isEmpty()) {
        // This is a leaf chapter
        return MdChapter(
            title = title,
            blocks = leadingBlocks,
            subChapters = emptyList(),
        )
      }

      val subChapters =
          generateSequence {
                val block = blockQueue.peek() ?: return@generateSequence null

                val followingHeading =
                    block as? Heading
                        ?: throw IllegalStateException(
                            "Expected heading, got ${block::class.simpleName}",
                        )

                val followingHeadingLevel = followingHeading.level

                when {
                  followingHeadingLevel <= leadHeadingLevel -> null

                  followingHeadingLevel == leadHeadingLevel + 1 -> {
                    blockQueue.pop()

                    MdChapter.load(blockQueue = blockQueue, leadHeading = followingHeading)
                  }

                  else ->
                      throw MdParseException(
                          "Invalid heading level $followingHeadingLevel; " +
                              " expected $leadHeadingLevel or ${leadHeadingLevel + 1}",
                      )
                }
              }
              .toList()

      return MdChapter(
          title = title,
          blocks = leadingBlocks,
          subChapters = subChapters,
      )
    }

    fun wrapper(
        title: List<MdInlineNode>,
        introBlocks: List<MdBlock> = emptyList(),
        subChapters: List<MdChapter>,
    ): MdChapter =
        MdChapter(
            title = MdInlineContent(title),
            blocks = introBlocks,
            subChapters = subChapters,
        )

    fun leaf(
        title: List<MdInlineNode>,
        blocks: List<MdBlock>,
    ): MdChapter =
        wrapper(
            title = title,
            introBlocks = blocks,
            subChapters = emptyList(),
        )
  }
}
