package software.medusa.commons.text

/** Wraps the text block that represents the entire contents of a text file. */
@JvmInline
value class TxtFileContent(
    val content: TxtBlock,
)
