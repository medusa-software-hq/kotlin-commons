package software.medusa.commons.markdown

import org.commonmark.node.Block

@JvmInline
value class MdElement(
    val blocks: List<MdBlock>,
) {
  internal fun dump(): List<Block> = blocks.map { it.dump() }

  /** Renders this element as a standalone Markdown fragment. */
  fun render(): String = MdDocument.render(element = this)

  /** Every inline node in this element's blocks, in document order. */
  fun visitInlineNodes(): Sequence<MdInlineNode> =
      blocks.asSequence().flatMap { it.visitInlineNodes() }

  companion object {
    val Empty = MdElement(emptyList())

    /**
     * Concatenates the blocks of [elements], in order, into a single element.
     *
     * Note: there is intentionally no `vararg` overload — `MdElement` is a value class, and Kotlin
     * prohibits varargs of value-class types.
     */
    fun concat(
        elements: List<MdElement>,
    ): MdElement = MdElement(blocks = elements.flatMap { element -> element.blocks })
  }
}
