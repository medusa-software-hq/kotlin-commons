package software.medusa.commons.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TxtApi_tests {
  @Test
  fun `line index exposes both numbering schemes`() {
    val index = TxtLineIndex.ofOneBased(3)

    assertEquals(2, index.indexZeroBased)
    assertEquals(3, index.indexOneBased)
    assertEquals(TxtLineIndex(indexZeroBased = 3), index.next)
  }

  @Test
  fun `block parse round-trips lf terminated content`() {
    val block = TxtBlock.parse("alpha\nbeta\n")

    assertEquals(listOf(TxtLine("alpha"), TxtLine("beta")), block.lines)
    assertEquals("alpha\nbeta\n", block.dump())
  }

  @Test
  fun `line detects indentation from leading spaces`() {
    assertEquals(4, TxtLine("    hello").detectIndentation())
  }

  @Test
  fun `patch rejects empty fragments`() {
    assertFailsWith<IllegalArgumentException> { TxtPatch(fragmentByOldLineIndexRange = emptyMap()) }
  }

  @Test
  fun `patch rejects adjacent ranges`() {
    val firstRange = TxtLineIndexRange.of(startIndex = TxtLineIndex.First, length = 1)
    val secondRange =
        TxtLineIndexRange.of(startIndex = TxtLineIndex(indexZeroBased = 1), length = 1)

    assertFailsWith<IllegalArgumentException> {
      TxtPatch(
          fragmentByOldLineIndexRange =
              mapOf(
                  firstRange to TxtPatch.Fragment.Empty,
                  secondRange to TxtPatch.Fragment(newContent = TxtBlock.of("replacement")),
              ),
      )
    }
  }

  @Test
  fun `file content wraps text block`() {
    val content = TxtFileContent(content = TxtBlock.of("hello"))

    assertEquals(TxtBlock.of("hello"), content.content)
  }

  @Test
  fun `applyPatch replaces a single line`() {
    val input =
        TxtBlock.of(
            "hello {",
            "  world {",
            "  }",
            "}",
        )

    val patched =
        input.applyPatch(
            patch =
                TxtPatch(
                    fragmentByOldLineIndexRange =
                        mapOf(
                            TxtLineIndexRange(
                                startIndex = TxtLineIndex(indexZeroBased = 1),
                                endIndexExclusive = TxtLineIndex(indexZeroBased = 2),
                            ) to
                                TxtPatch.Fragment(
                                    newContent = TxtBlock.of("  universe {"),
                                ),
                        ),
                ),
        )

    assertEquals(
        TxtBlock.of(
            "hello {",
            "  universe {",
            "  }",
            "}",
        ),
        patched,
    )
  }

  @Test
  fun `applyPatch can expand a replacement`() {
    val input =
        TxtBlock.of(
            "hello {",
            "  world {",
            "  }",
            "}",
        )

    val patched =
        input.applyPatch(
            patch =
                TxtPatch(
                    fragmentByOldLineIndexRange =
                        mapOf(
                            TxtLineIndexRange(
                                startIndex = TxtLineIndex(indexZeroBased = 1),
                                endIndexExclusive = TxtLineIndex(indexZeroBased = 2),
                            ) to
                                TxtPatch.Fragment(
                                    newContent =
                                        TxtBlock.of(
                                            "  universe {",
                                            "    and beyond",
                                        ),
                                ),
                        ),
                ),
        )

    assertEquals(
        TxtBlock.of(
            "hello {",
            "  universe {",
            "    and beyond",
            "  }",
            "}",
        ),
        patched,
    )
  }

  @Test
  fun `applyPatch can delete middle range`() {
    val input =
        TxtBlock.of(
            "hello {{",
            "  world [",
            "  ]",
            "}}",
        )

    val patched =
        input.applyPatch(
            patch =
                TxtPatch(
                    fragmentByOldLineIndexRange =
                        mapOf(
                            TxtLineIndexRange(
                                startIndex = TxtLineIndex(indexZeroBased = 1),
                                endIndexExclusive = TxtLineIndex(indexZeroBased = 3),
                            ) to TxtPatch.Fragment.Empty,
                        ),
                ),
        )

    assertEquals(
        TxtBlock.of(
            "hello {{",
            "}}",
        ),
        patched,
    )
  }
}
