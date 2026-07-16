package software.medusa.commons.openai_client.tools

/**
 * The name of a tool (function) the model may call.
 *
 * Constrained to the shape OpenAI accepts: 1-64 characters, each of which is `a-z`, `A-Z`, `0-9`,
 * an underscore, or a dash.
 */
@JvmInline
value class OaiToolName(
    val name: String,
) {
  init {
    require(name.isNotEmpty()) { "Tool name must not be empty" }
    require(name.length <= MAX_LENGTH) {
      "Tool name must be at most $MAX_LENGTH characters, but was ${name.length}: '$name'"
    }
    require(name.all(::isAllowedChar)) {
      "Tool name may only contain a-z, A-Z, 0-9, underscores and dashes, but was: '$name'"
    }
  }

  companion object {
    private const val MAX_LENGTH = 64

    private fun isAllowedChar(char: Char): Boolean =
        char in 'a'..'z' || char in 'A'..'Z' || char in '0'..'9' || char == '_' || char == '-'
  }
}
