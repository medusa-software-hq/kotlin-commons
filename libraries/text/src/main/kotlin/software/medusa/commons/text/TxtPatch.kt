package software.medusa.commons.text

/** Patch description for a [TxtBlock]. */
data class TxtPatch(
    val fragmentByOldLineIndexRange: Map<TxtLineIndexRange, Fragment>,
) {
  /** Replacement content for a single patched range. */
  @JvmInline
  value class Fragment(
      val newContent: TxtBlock,
  ) {
    companion object {
      val Empty = Fragment(newContent = TxtBlock.Empty)
    }
  }

  init {
    require(fragmentByOldLineIndexRange.isNotEmpty()) {
      "A text patch must contain at least one fragment"
    }

    val isStructuredCorrectly =
        fragmentByOldLineIndexRange.entries
            .sortedBy { it.key.startIndex }
            .zipWithNext()
            .none { (previousEntry, nextEntry) -> previousEntry.key.collides(nextEntry.key) }

    require(isStructuredCorrectly) { "Line index ranges in the patch must not collide" }
  }

  companion object {
    fun merge(
        patches: Iterable<TxtPatch>,
    ): TxtPatch =
        TxtPatch(
            fragmentByOldLineIndexRange =
                patches
                    .flatMap { patch -> patch.fragmentByOldLineIndexRange.entries }
                    .associate { it.toPair() },
        )
  }

  val spanLineIndexRange: TxtLineIndexRange by lazy {
    TxtLineIndexRange(
        startIndex = fragmentByOldLineIndexRange.keys.minOf { it.startIndex },
        endIndexExclusive = fragmentByOldLineIndexRange.keys.maxOf { it.endIndexExclusive },
    )
  }
}
