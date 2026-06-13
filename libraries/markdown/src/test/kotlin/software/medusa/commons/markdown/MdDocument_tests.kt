package software.medusa.commons.markdown

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MdDocument_tests {
  @Test
  fun toMarkdownString_roundTripsSupportedDocument() {
    val document =
        MdDocument(
            rootChapter =
                MdChapter(
                    title =
                        MdInlineContent(
                            listOf(
                                MdInlineNode.Text("Welcome to "),
                                MdInlineNode.Link(
                                    destination = "https://example.com/docs path",
                                    title = "Docs",
                                    content =
                                        listOf(
                                            MdInlineNode.Strong(
                                                listOf(MdInlineNode.Text("Medusa")),
                                            ),
                                        ),
                                ),
                            ),
                        ),
                    blocks =
                        listOf(
                            MdBlock.Paragraph(
                                content =
                                    MdInlineContent(
                                        listOf(
                                            MdInlineNode.Text("Intro with "),
                                            MdInlineNode.Code("code"),
                                            MdInlineNode.Text(" and "),
                                            MdInlineNode.Emphasis(
                                                listOf(MdInlineNode.Text("focus")),
                                            ),
                                            MdInlineNode.HardBreak,
                                            MdInlineNode.Text("second line"),
                                        ),
                                    ),
                            ),
                            MdBlock.ListBlock(
                                topLevel =
                                    MdBlock.ListBlock.Level(
                                        items =
                                            listOf(
                                                MdBlock.ListBlock.Item(
                                                    content =
                                                        MdInlineContent(
                                                            listOf(
                                                                MdInlineNode.Text("first bullet"),
                                                            ),
                                                        ),
                                                    nestedLevel = null,
                                                ),
                                                MdBlock.ListBlock.Item(
                                                    content =
                                                        MdInlineContent(
                                                            listOf(
                                                                MdInlineNode.Text("parent bullet"),
                                                            ),
                                                        ),
                                                    nestedLevel =
                                                        MdBlock.ListBlock.Level(
                                                            items =
                                                                listOf(
                                                                    MdBlock.ListBlock.Item(
                                                                        content =
                                                                            MdInlineContent(
                                                                                listOf(
                                                                                    MdInlineNode
                                                                                        .Text(
                                                                                            "nested bullet",
                                                                                        ),
                                                                                ),
                                                                            ),
                                                                        nestedLevel = null,
                                                                    ),
                                                                ),
                                                        ),
                                                ),
                                            ),
                                    ),
                            ),
                            MdBlock.CodeBlock(
                                code = "val ticks = \"```\"\n",
                                info = "kotlin",
                            ),
                            MdBlock.RawCodeBlock(
                                code = "literal **markdown**\n",
                            ),
                        ),
                    subChapters =
                        listOf(
                            MdChapter(
                                title = MdInlineContent(listOf(MdInlineNode.Text("Child"))),
                                blocks =
                                    listOf(
                                        MdBlock.Paragraph(
                                            content =
                                                MdInlineContent(
                                                    listOf(
                                                        MdInlineNode.Text("child paragraph"),
                                                    ),
                                                ),
                                        ),
                                    ),
                                subChapters = emptyList(),
                            ),
                        ),
                ),
        )

    assertEquals(document, MdDocument.parse(document.render()))
  }

  @Test
  fun toMarkdownString_escapesParagraphStartThatWouldBecomeList() {
    val document =
        MdDocument(
            rootChapter =
                MdChapter(
                    title = MdInlineContent(listOf(MdInlineNode.Text("Root"))),
                    blocks =
                        listOf(
                            MdBlock.Paragraph(
                                content =
                                    MdInlineContent(
                                        listOf(
                                            MdInlineNode.Text("- bullet?"),
                                        ),
                                    ),
                            ),
                        ),
                    subChapters = emptyList(),
                ),
        )

    assertEquals(document, MdDocument.parse(document.render()))
  }

  @Test
  fun toMarkdownString_roundTripsCodeSpanContainingBackticks() {
    val document =
        MdDocument(
            rootChapter =
                MdChapter(
                    title = MdInlineContent(listOf(MdInlineNode.Text("Root"))),
                    blocks =
                        listOf(
                            MdBlock.Paragraph(
                                content =
                                    MdInlineContent(
                                        listOf(
                                            MdInlineNode.Text("prefix "),
                                            MdInlineNode.Code("has `` inside"),
                                            MdInlineNode.Text(" suffix"),
                                        ),
                                    ),
                            ),
                            MdBlock.CodeBlock(
                                code = "````\ncontent\n````\n",
                                info = null,
                            ),
                        ),
                    subChapters = emptyList(),
                ),
        )

    assertEquals(document, MdDocument.parse(document.render()))
  }

  @Test
  fun parse_buildsChapterTreeWithIntroBlocksAndInlineContent() {
    val document =
        MdDocument.parse(
            """
            # Welcome to [Medusa](https://example.com)

            Intro with `code` and *focus*.

            ```kotlin
            val answer = 42
            ```

            ## Getting Started

            Start with **confidence**.

            ### Install

            Use [the guide](https://example.com/install "Install").
            """
                .trimIndent(),
        )

    assertEquals(
        MdDocument(
            rootChapter =
                MdChapter(
                    title =
                        MdInlineContent(
                            listOf(
                                MdInlineNode.Text("Welcome to "),
                                MdInlineNode.Link(
                                    destination = "https://example.com",
                                    title = null,
                                    content = listOf(MdInlineNode.Text("Medusa")),
                                ),
                            ),
                        ),
                    blocks =
                        listOf(
                            MdBlock.Paragraph(
                                content =
                                    MdInlineContent(
                                        listOf(
                                            MdInlineNode.Text("Intro with "),
                                            MdInlineNode.Code("code"),
                                            MdInlineNode.Text(" and "),
                                            MdInlineNode.Emphasis(
                                                listOf(MdInlineNode.Text("focus")),
                                            ),
                                            MdInlineNode.Text("."),
                                        ),
                                    ),
                            ),
                            MdBlock.CodeBlock(
                                code = "val answer = 42\n",
                                info = "kotlin",
                            ),
                        ),
                    subChapters =
                        listOf(
                            MdChapter(
                                title =
                                    MdInlineContent(listOf(MdInlineNode.Text("Getting Started"))),
                                blocks =
                                    listOf(
                                        MdBlock.Paragraph(
                                            content =
                                                MdInlineContent(
                                                    listOf(
                                                        MdInlineNode.Text("Start with "),
                                                        MdInlineNode.Strong(
                                                            listOf(MdInlineNode.Text("confidence")),
                                                        ),
                                                        MdInlineNode.Text("."),
                                                    ),
                                                ),
                                        ),
                                    ),
                                subChapters =
                                    listOf(
                                        MdChapter(
                                            title =
                                                MdInlineContent(
                                                    listOf(MdInlineNode.Text("Install"))
                                                ),
                                            blocks =
                                                listOf(
                                                    MdBlock.Paragraph(
                                                        content =
                                                            MdInlineContent(
                                                                listOf(
                                                                    MdInlineNode.Text("Use "),
                                                                    MdInlineNode.Link(
                                                                        destination =
                                                                            "https://example.com/install",
                                                                        title = "Install",
                                                                        content =
                                                                            listOf(
                                                                                MdInlineNode.Text(
                                                                                    "the guide",
                                                                                ),
                                                                            ),
                                                                    ),
                                                                    MdInlineNode.Text("."),
                                                                ),
                                                            ),
                                                    ),
                                                ),
                                            subChapters = emptyList(),
                                        ),
                                    ),
                            ),
                        ),
                ),
        ),
        document,
    )
  }

  @Test
  fun parse_supportsBulletLists() {
    val document =
        MdDocument.parse(
            """
            # Root

            - first item
            - second item with `code`
            """
                .trimIndent(),
        )

    assertEquals(
        MdDocument(
            rootChapter =
                MdChapter(
                    title = MdInlineContent(listOf(MdInlineNode.Text("Root"))),
                    blocks =
                        listOf(
                            MdBlock.ListBlock(
                                topLevel =
                                    MdBlock.ListBlock.Level(
                                        items =
                                            listOf(
                                                MdBlock.ListBlock.Item(
                                                    content =
                                                        MdInlineContent(
                                                            listOf(MdInlineNode.Text("first item"))
                                                        ),
                                                    nestedLevel = null,
                                                ),
                                                MdBlock.ListBlock.Item(
                                                    content =
                                                        MdInlineContent(
                                                            listOf(
                                                                MdInlineNode.Text(
                                                                    "second item with ",
                                                                ),
                                                                MdInlineNode.Code("code"),
                                                            ),
                                                        ),
                                                    nestedLevel = null,
                                                ),
                                            ),
                                    ),
                            ),
                        ),
                    subChapters = emptyList(),
                ),
        ),
        document,
    )
  }

  @Test
  fun parse_supportsNestedListsInsideListItems() {
    val document =
        MdDocument.parse(
            """
            # Root

            - parent item
              - nested child
            """
                .trimIndent(),
        )

    assertEquals(
        MdDocument(
            rootChapter =
                MdChapter(
                    title = MdInlineContent(listOf(MdInlineNode.Text("Root"))),
                    blocks =
                        listOf(
                            MdBlock.ListBlock(
                                topLevel =
                                    MdBlock.ListBlock.Level(
                                        items =
                                            listOf(
                                                MdBlock.ListBlock.Item(
                                                    content =
                                                        MdInlineContent(
                                                            listOf(
                                                                MdInlineNode.Text("parent item"),
                                                            ),
                                                        ),
                                                    nestedLevel =
                                                        MdBlock.ListBlock.Level(
                                                            items =
                                                                listOf(
                                                                    MdBlock.ListBlock.Item(
                                                                        content =
                                                                            MdInlineContent(
                                                                                listOf(
                                                                                    MdInlineNode
                                                                                        .Text(
                                                                                            "nested child",
                                                                                        ),
                                                                                ),
                                                                            ),
                                                                        nestedLevel = null,
                                                                    ),
                                                                ),
                                                        ),
                                                ),
                                            ),
                                    ),
                            ),
                        ),
                    subChapters = emptyList(),
                ),
        ),
        document,
    )
  }

  @Test
  fun parse_supportsRawCcCodeBlocks() {
    val document =
        MdDocument.parse(
            buildString {
              appendLine("# Root")
              appendLine()
              appendLine(ControlChar.STX)
              appendLine("first line")
              appendLine()
              appendLine("second line with ``` and **literal** text")
              append(ControlChar.ETX)
            },
        )

    assertEquals(
        MdDocument(
            rootChapter =
                MdChapter(
                    title = MdInlineContent(listOf(MdInlineNode.Text("Root"))),
                    blocks =
                        listOf(
                            MdBlock.RawCodeBlock(
                                code =
                                    """
                                    first line

                                    second line with ``` and **literal** text
                                    """
                                        .trimIndent() + "\n",
                            ),
                        ),
                    subChapters = emptyList(),
                ),
        ),
        document,
    )
  }

  @Test
  fun parse_supportsEmptyRawCcCodeBlocks() {
    val document =
        MdDocument.parse(
            buildString {
              appendLine("# Root")
              appendLine()
              appendLine(ControlChar.STX)
              append(ControlChar.ETX)
            },
        )

    assertEquals(
        MdDocument(
            rootChapter =
                MdChapter(
                    title = MdInlineContent(listOf(MdInlineNode.Text("Root"))),
                    blocks = listOf(MdBlock.RawCodeBlock(code = "")),
                    subChapters = emptyList(),
                ),
        ),
        document,
    )
  }

  @Test
  fun parse_rawCcCodeBlockKeepsFencedCodeSyntaxLiteral() {
    val document =
        MdDocument.parse(
            buildString {
              appendLine("# Root")
              appendLine()
              appendLine(ControlChar.STX)
              appendLine("```java")
              appendLine("code")
              appendLine("```")
              append(ControlChar.ETX)
            },
        )

    assertEquals(
        MdDocument(
            rootChapter =
                MdChapter(
                    title = MdInlineContent(listOf(MdInlineNode.Text("Root"))),
                    blocks =
                        listOf(
                            MdBlock.RawCodeBlock(
                                code =
                                    """
                                    ```java
                                    code
                                    ```
                                    """
                                        .trimIndent() + "\n",
                            ),
                        ),
                    subChapters = emptyList(),
                ),
        ),
        document,
    )
  }

  @Test
  fun parse_rawCcCodeBlockWithNonClosingEtxLineKeepsThatLineLiteral() {
    val document =
        MdDocument.parse(
            buildString {
              appendLine("# Root")
              appendLine()
              appendLine(ControlChar.STX)
              appendLine("code")
              append(ControlChar.ETX)
              appendLine(" a")
            },
        )

    assertEquals(
        MdDocument(
            rootChapter =
                MdChapter(
                    title = MdInlineContent(listOf(MdInlineNode.Text("Root"))),
                    blocks =
                        listOf(
                            MdBlock.RawCodeBlock(
                                code = "code\n${ControlChar.ETX} a\n",
                            ),
                        ),
                    subChapters = emptyList(),
                ),
        ),
        document,
    )
  }

  @Test
  fun parse_rawCcCodeBlockMayBeIndentedUpToThreeSpaces() {
    val document =
        MdDocument.parse(
            buildString {
              appendLine("# Root")
              appendLine()
              append("   ")
              appendLine(ControlChar.STX)
              appendLine("code")
              append("   ")
              append(ControlChar.ETX)
            },
        )

    assertEquals(
        MdDocument(
            rootChapter =
                MdChapter(
                    title = MdInlineContent(listOf(MdInlineNode.Text("Root"))),
                    blocks = listOf(MdBlock.RawCodeBlock(code = "code\n")),
                    subChapters = emptyList(),
                ),
        ),
        document,
    )
  }

  @Test
  fun parse_fourSpaceIndentedStxThrowsBecauseIndentedCodeBlocksAreUnsupported() {
    assertFailsWith<MdParseException> {
      MdDocument.parse(
          buildString {
            appendLine("# Root")
            appendLine()
            append("    ")
            appendLine(ControlChar.STX)
            appendLine("code")
            append(ControlChar.ETX)
          },
      )
    }
  }

  @Test
  fun parse_rawCcCodeBlockClosingIndentedTooFarDoesNotClose() {
    val document =
        MdDocument.parse(
            buildString {
              appendLine("# Root")
              appendLine()
              appendLine(ControlChar.STX)
              appendLine("code")
              append("    ")
              appendLine(ControlChar.ETX)
              append("end")
            },
        )

    assertEquals(
        MdDocument(
            rootChapter =
                MdChapter(
                    title = MdInlineContent(listOf(MdInlineNode.Text("Root"))),
                    blocks =
                        listOf(
                            MdBlock.RawCodeBlock(
                                code = "code\n    ${ControlChar.ETX}\nend\n",
                            ),
                        ),
                    subChapters = emptyList(),
                ),
        ),
        document,
    )
  }

  @Test
  fun parse_rejectsHeadingLevelSkips() {
    assertFailsWith<MdParseException> {
      MdDocument.parse(
          """
          # Root

          ### Too Deep
          """
              .trimIndent(),
      )
    }
  }

  @Test
  fun parse_rejectsDocumentWithoutTopLevelHeading() {
    assertFailsWith<MdParseException> { MdDocument.parse("Paragraph before heading") }
  }

  @Test
  fun parse_rejectsNestedHeadingThatSkipsOneLevel() {
    assertFailsWith<MdParseException> {
      MdDocument.parse(
          """
          # Root

          ## Child

          #### Too Deep
          """
              .trimIndent(),
      )
    }
  }

  @Test
  fun parse_rejectsUnsupportedTopLevelStructures() {
    assertFailsWith<MdParseException> {
      MdDocument.parse(
          """
          # Root

          > quoted
          """
              .trimIndent(),
      )
    }
  }

  @Test
  fun parse_rejectsUnsupportedInlineStructures() {
    assertFailsWith<MdParseException> {
      MdDocument.parse(
          """
          # Root ![alt](https://example.com/image.png)
          """
              .trimIndent(),
      )
    }
  }
}
