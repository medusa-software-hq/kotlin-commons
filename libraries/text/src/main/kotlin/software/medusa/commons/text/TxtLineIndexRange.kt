package software.medusa.commons.text

/** Inclusive-exclusive range of line indices in a [TxtBlock]. */
data class TxtLineIndexRange(
    val startIndex: TxtLineIndex,
    val endIndexExclusive: TxtLineIndex,
) {
  companion object {
    fun empty(startIndex: TxtLineIndex): TxtLineIndexRange =
        TxtLineIndexRange(
            startIndex = startIndex,
            endIndexExclusive = startIndex,
        )

    fun of(
        startIndex: TxtLineIndex,
        length: Int,
    ): TxtLineIndexRange {
      require(length >= 0) { "Line index range length must be non-negative" }

      return TxtLineIndexRange(
          startIndex = startIndex,
          endIndexExclusive = TxtLineIndex(indexZeroBased = startIndex.indexZeroBased + length),
      )
    }
  }

  init {
    require(startIndex <= endIndexExclusive) {
      "Start line index must be less than or equal to end line index"
    }
  }

  fun collides(other: TxtLineIndexRange): Boolean =
      startIndex <= other.endIndexExclusive && endIndexExclusive >= other.startIndex

  fun overlaps(other: TxtLineIndexRange): Boolean =
      startIndex < other.endIndexExclusive && endIndexExclusive > other.startIndex
}
