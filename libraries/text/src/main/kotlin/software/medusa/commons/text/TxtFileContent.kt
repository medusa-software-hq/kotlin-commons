package software.medusa.commons.text

import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.asReadOnlyByteBuffer

/** Wraps the text block that represents the entire contents of a text file. */
@JvmInline
value class TxtFileContent(
    val content: TxtBlock,
) {
  companion object {
    private val decoder =
        StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)

    fun decode(
        byteContent: ByteString,
    ): TxtFileContent? =
        try {
          val rawContent = decoder.decode(byteContent.asReadOnlyByteBuffer()).toString()
          val blockContent = TxtBlock.parse(rawContent = rawContent)

          TxtFileContent(
              content = blockContent,
          )
        } catch (_: CharacterCodingException) {
          null
        }
  }

  val indexedLines: Sequence<TxtBlock.IndexedLine>
    get() =
        content.buildIndexedLines(
            baseIndex = TxtLineIndex.First,
        )

  fun dump(): String = content.dump()
}
