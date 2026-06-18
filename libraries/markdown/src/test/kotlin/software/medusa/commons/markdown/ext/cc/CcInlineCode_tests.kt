package software.medusa.commons.markdown.ext.cc

import kotlin.test.Test
import kotlin.test.assertEquals
import org.commonmark.Extension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import software.medusa.commons.markdown.ControlChar

class CcInlineCode_tests {
  companion object {
    private val extensions: Set<Extension> = setOf(CcExtension.create())
    private val markdownParser: Parser = Parser.builder().extensions(extensions).build()
    private val htmlRenderer: HtmlRenderer = HtmlRenderer.builder().extensions(extensions).build()
  }

  private fun assertRendering(input: String, expected: String) {
    assertEquals(expected, render(input), "Rendering of: $input")
  }

  private fun render(source: String): String = htmlRenderer.render(markdownParser.parse(source))

  @Test
  fun basicBlock() {
    val input = "${ControlChar.STX}\ncode\n${ControlChar.ETX}"

    val document = markdownParser.parse(input)
    val codeBlock = document.firstChild as CcCodeBlock
    assertEquals("code\n", codeBlock.literal)
    assertRendering(input, "<pre><code>code\n</code></pre>\n")
  }

  @Test
  fun inlineCode() {
    val input = "before ${ControlChar.SO}foo${ControlChar.SI} after"

    val document = markdownParser.parse(input)
    val inlineCode = document.firstChild!!.firstChild!!.next as CcInlineCode
    assertEquals("foo", inlineCode.literal)
    assertRendering(input, "<p>before <code>foo</code> after</p>\n")
  }

  @Test
  fun emptyInlineCode() {
    assertRendering("x ${ControlChar.SO}${ControlChar.SI} y", "<p>x <code></code> y</p>\n")
  }

  @Test
  fun unmatchedOpeningInlineCodeFallsBackToLiteralText() {
    assertRendering("x ${ControlChar.SO}foo", "<p>x ${ControlChar.SO}foo</p>\n")
  }

  @Test
  fun inlineCodeCanContainBackticks() {
    assertRendering(
        "x ${ControlChar.SO}`foo`${ControlChar.SI} y",
        "<p>x <code>`foo`</code> y</p>\n",
    )
  }

  @Test
  fun regularBackticksStillWork() {
    assertRendering("x `foo` y", "<p>x <code>foo</code> y</p>\n")
  }

  @Test
  fun inlineCodeInsideCcBlockStaysLiteral() {
    val input = "${ControlChar.STX}\n${ControlChar.SO}foo${ControlChar.SI}\n${ControlChar.ETX}"

    val document = markdownParser.parse(input)
    val codeBlock = document.firstChild as CcCodeBlock
    assertEquals("${ControlChar.SO}foo${ControlChar.SI}\n", codeBlock.literal)
    assertRendering(input, "<pre><code>${ControlChar.SO}foo${ControlChar.SI}\n</code></pre>\n")
  }

  @Test
  fun emptyBlock() {
    assertRendering("${ControlChar.STX}\n${ControlChar.ETX}", "<pre><code></code></pre>\n")
  }

  @Test
  fun supportsFencedCodeBlockSyntaxInside() {
    val input = "${ControlChar.STX}\n```java\ncode\n```\n${ControlChar.ETX}"

    val document = markdownParser.parse(input)
    val codeBlock = document.firstChild as CcCodeBlock
    assertEquals("```java\ncode\n```\n", codeBlock.literal)
    assertRendering(input, "<pre><code>```java\ncode\n```\n</code></pre>\n")
  }

  @Test
  fun closingCanHaveSpacesAfter() {
    assertRendering(
        "${ControlChar.STX}\ncode\n${ControlChar.ETX}   ",
        "<pre><code>code\n</code></pre>\n",
    )
  }

  @Test
  fun closingCanNotHaveNonSpaces() {
    val input = "${ControlChar.STX}\ncode\n${ControlChar.ETX} a"

    val document = markdownParser.parse(input)
    val codeBlock = document.firstChild as CcCodeBlock
    assertEquals("code\n${ControlChar.ETX} a\n", codeBlock.literal)
    assertRendering(input, "<pre><code>code\n${ControlChar.ETX} a\n</code></pre>\n")
  }

  @Test
  fun unterminatedBlockRunsToEndOfDocument() {
    val input = "${ControlChar.STX}\ncode\nmore"

    val document = markdownParser.parse(input)
    val codeBlock = document.firstChild as CcCodeBlock
    assertEquals("code\nmore\n", codeBlock.literal)
    assertRendering(input, "<pre><code>code\nmore\n</code></pre>\n")
  }

  @Test
  fun etxAloneDoesNotStartBlock() {
    assertRendering("${ControlChar.ETX}\ntext", "<p>${ControlChar.ETX}\ntext</p>\n")
  }

  @Test
  fun openingAllowsUpToThreeSpacesIndent() {
    assertRendering(
        "   ${ControlChar.STX}\ncode\n   ${ControlChar.ETX}",
        "<pre><code>code\n</code></pre>\n",
    )
  }

  @Test
  fun fourSpaceIndentDoesNotOpenBlock() {
    assertRendering(
        "    ${ControlChar.STX}\ncode\n${ControlChar.ETX}",
        "<pre><code>${ControlChar.STX}\n</code></pre>\n<p>code\n${ControlChar.ETX}</p>\n",
    )
  }

  @Test
  fun closingIndentedTooFarDoesNotClose() {
    val input = "${ControlChar.STX}\ncode\n    ${ControlChar.ETX}\nend"

    val document = markdownParser.parse(input)
    val codeBlock = document.firstChild as CcCodeBlock
    assertEquals("code\n    ${ControlChar.ETX}\nend\n", codeBlock.literal)
    assertRendering(input, "<pre><code>code\n    ${ControlChar.ETX}\nend\n</code></pre>\n")
  }
}
