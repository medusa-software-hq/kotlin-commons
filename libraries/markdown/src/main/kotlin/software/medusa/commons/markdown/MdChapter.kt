package software.medusa.commons.markdown

import org.commonmark.node.Block
import org.commonmark.node.Heading
import org.commonmark.node.Node
import software.medusa.commons.markdown.utils.PoppableQueue
import software.medusa.commons.markdown.utils.popOfWhileNotNull

data class MdChapter(
    val title: MdInlineContent,
    val element: MdElement,
    val subChapters: List<MdChapter>,
) {
  internal fun dump(level: Int): List<Node> = buildList {
    add(
        Heading().also { heading ->
          heading.level = level
          title.dump().forEach(heading::appendChild)
        },
    )
    addAll(element.dump())
    subChapters.forEach { subChapter -> addAll(subChapter.dump(level = level + 1)) }
  }

  fun render(): String =
      MdDocument(
              rootChapter = this,
          )
          .render()

  /**
   * Every inline node in this chapter — its title, its element, and all its sub-chapters — in
   * document order.
   */
  fun visitInlineNodes(): Sequence<MdInlineNode> =
      title.visitInlineNodes() +
          element.visitInlineNodes() +
          subChapters.asSequence().flatMap { it.visitInlineNodes() }

  fun replaceTitle(
      newTitle: MdInlineContent,
  ): MdChapter =
      copy(
          title = newTitle,
      )

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
            element = MdElement(leadingBlocks),
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
          element = MdElement(leadingBlocks),
          subChapters = subChapters,
      )
    }

    fun wrapper(
        title: MdInlineContent,
        introElement: MdElement = MdElement.Empty,
        subChapters: List<MdChapter>,
    ): MdChapter =
        MdChapter(
            title = title,
            element = introElement,
            subChapters = subChapters,
        )

    fun leaf(
        title: MdInlineContent,
        element: MdElement,
    ): MdChapter =
        MdChapter(
            title = title,
            element = element,
            subChapters = emptyList(),
        )
  }
}
