package software.medusa.commons.openai_client

import com.aallam.openai.api.core.FinishReason

/**
 * Decides what usable text (if any) a chat response's first choice yields, given the three facts
 * that matter: whether there was a choice at all, its finish reason, and its message content.
 *
 * Extracted from the SDK types so it's unit-testable without constructing a whole `ChatCompletion`.
 *
 * Rules:
 * - no choice → [OaiEmptyResponseException]
 * - an explicit finish reason other than `stop` (e.g. `length`, `content_filter`) →
 *   [OaiIncompleteResponseException], *even if partial content was produced* — truncated/filtered
 *   output must never be returned as if it were a finished answer.
 * - null content otherwise → [OaiEmptyResponseException]
 * - a null finish reason is tolerated as long as content is present: some providers omit it, and
 *   failing on that would regress those providers.
 */
internal fun interpretResponseContent(
    hasAnyChoice: Boolean,
    finishReason: String?,
    messageContent: String?,
): String {
  // A non-stop finish reason means truncated/filtered output — reject it before looking at content,
  // so partial content is never returned as if finished. (No choice ⇒ finishReason is null here,
  // so this is skipped and the empty-response branch below handles it.)
  if (finishReason != null && finishReason != FinishReason.Stop.value) {
    throw OaiIncompleteResponseException(
        finishReason = finishReason,
        message =
            "OpenAI response was cut short (finish_reason=$finishReason); its content is " +
                "incomplete and was discarded",
    )
  }

  return messageContent
      ?: throw OaiEmptyResponseException(
          if (hasAnyChoice) "OpenAI response choice did not contain text content"
          else "OpenAI response did not contain any choices",
      )
}
