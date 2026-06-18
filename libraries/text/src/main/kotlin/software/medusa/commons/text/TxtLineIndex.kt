package software.medusa.commons.text

/** Zero-based index of a line within a [TxtBlock]. */
@JvmInline
value class TxtLineIndex(
    val indexZeroBased: Int,
) : Comparable<TxtLineIndex> {
  companion object {
    val First = TxtLineIndex(indexZeroBased = 0)

    fun ofOneBased(indexOneBased: Int): TxtLineIndex {
      require(indexOneBased > 0) { "One-based line index must be positive" }
      return TxtLineIndex(indexZeroBased = indexOneBased - 1)
    }
  }

  init {
    require(indexZeroBased >= 0) { "Line index must be non-negative" }
  }

  val indexOneBased: Int
    get() = indexZeroBased + 1

  val next: TxtLineIndex
    get() = TxtLineIndex(indexZeroBased = indexZeroBased + 1)

  override fun compareTo(other: TxtLineIndex): Int =
      compareValuesBy(this, other) { it.indexZeroBased }
}
