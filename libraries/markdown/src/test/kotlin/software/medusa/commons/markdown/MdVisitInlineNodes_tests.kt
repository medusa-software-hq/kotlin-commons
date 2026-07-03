package software.medusa.commons.markdown

import kotlin.test.Test
import kotlin.test.assertEquals

class MdVisitInlineNodes_tests {
  @Test
  fun `visitInlineNodes walks titles, paragraphs, lists and nested inline nodes, skipping code blocks`() {
    val document =
        MdDocument.parse(
            """
            # Title with `titleCode`

            A paragraph mentioning `/src/Main.kt` and **strong `nestedCode`**.

            - item with `/build.gradle.kts`
              - deeper `/gradle/libs.versions.toml`

            ```
            `notInlineCode` inside a fenced block
            ```
            """
                .trimIndent(),
        )

    val inlineCodes =
        document.visitInlineNodes().filterIsInstance<MdInlineNode.Code>().map { it.code }.toList()

    assertEquals(
        expected =
            listOf(
                "titleCode",
                "/src/Main.kt",
                // The code nested inside the strong node is reached by the deep walk.
                "nestedCode",
                "/build.gradle.kts",
                "/gradle/libs.versions.toml",
            ),
        actual = inlineCodes,
    )
  }
}
