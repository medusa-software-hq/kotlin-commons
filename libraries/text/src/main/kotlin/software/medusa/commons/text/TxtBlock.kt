package software.medusa.commons.text

/** A multi-line text block, similar in spirit to the old `CodeBlock`. */
data class TxtBlock(
    val lines: List<TxtLine>,
) {
  data class IndexedLine(
      val index: TxtLineIndex,
      val line: TxtLine,
  )

  companion object {
    val Empty = TxtBlock(lines = emptyList())

    fun of(vararg lines: String): TxtBlock = of(lines = lines.toList())

    fun of(lines: List<String>): TxtBlock = TxtBlock(lines = lines.map(::TxtLine))

    /**
     * Parses [rawContent] into lines, trimming a trailing LF in the same way as the old code model.
     */
    fun parse(rawContent: String): TxtBlock {
      val strippedRawContent =
          when {
            rawContent.endsWith('\n') -> rawContent.dropLast(1)
            else -> rawContent
          }

      return TxtBlock(lines = strippedRawContent.split('\n').map(::TxtLine))
    }
  }

  val height: Int
    get() = lines.size

  /** Dumps this block as LF-terminated text. */
  fun dump(): String = lines.joinToString(separator = "") { "${it.content}\n" }

  fun buildIndexedLines(
      baseIndex: TxtLineIndex = TxtLineIndex.First,
  ): Sequence<IndexedLine> =
      lines.asSequence().mapIndexed { indexZeroBased, line ->
        IndexedLine(
            index = TxtLineIndex(indexZeroBased = baseIndex.indexZeroBased + indexZeroBased),
            line = line,
        )
      }

  /** Applies [patch] to this block. */
  fun applyPatch(
      patch: TxtPatch,
      baseLineIndex: TxtLineIndex = TxtLineIndex.First,
  ): TxtBlock {
    val originalLinesByIndex =
        buildIndexedLines(baseIndex = baseLineIndex).associate { indexedLine ->
          indexedLine.index to indexedLine.line
        }

    val patchedLines = mutableListOf<TxtLine>()
    var cursorIndex = baseLineIndex

    patch.fragmentByOldLineIndexRange
        .toSortedMap(compareBy<TxtLineIndexRange> { it.startIndex })
        .forEach { (range, fragment) ->
          while (cursorIndex < range.startIndex) {
            patchedLines +=
                checkNotNull(originalLinesByIndex[cursorIndex]) {
                  "Patch start index $cursorIndex is outside the block"
                }
            cursorIndex = cursorIndex.next
          }

          cursorIndex = range.endIndexExclusive
          patchedLines += fragment.newContent.lines
        }

    val endExclusiveIndex = TxtLineIndex(indexZeroBased = baseLineIndex.indexZeroBased + lines.size)

    while (cursorIndex < endExclusiveIndex) {
      patchedLines +=
          checkNotNull(originalLinesByIndex[cursorIndex]) {
            "Patch end index $cursorIndex is outside the block"
          }
      cursorIndex = cursorIndex.next
    }

    return TxtBlock(lines = patchedLines)
  }
}
