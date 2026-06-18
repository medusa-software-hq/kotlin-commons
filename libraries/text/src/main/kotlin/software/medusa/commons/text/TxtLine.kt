package software.medusa.commons.text

/** A single line of text. */
@JvmInline
value class TxtLine(
    val content: String,
) {
  /** Detects indentation by counting leading spaces. */
  fun detectIndentation(): Int = content.takeWhile { it == ' ' }.length
}
