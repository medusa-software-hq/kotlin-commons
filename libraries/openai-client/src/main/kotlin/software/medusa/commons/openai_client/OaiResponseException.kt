package software.medusa.commons.openai_client

/**
 * A model response that couldn't be turned into usable content. Distinct subtypes so callers can
 * react differently — e.g. retry a transient empty response, but surface a truncation as "raise the
 * token budget / shrink the prompt" rather than retrying it blindly.
 */
sealed class OaiResponseException(
    message: String,
) : RuntimeException(message)

/**
 * The response carried no usable text — no choices at all, or a choice whose message content was
 * null while the model reported a normal (or absent) finish reason. Typically transient and
 * provider-driven (e.g. a flaky upstream behind OpenRouter); re-issuing the request usually clears
 * it.
 */
class OaiEmptyResponseException(
    message: String,
) : OaiResponseException(message)

/**
 * The model stopped for a reason other than `stop` — `length` (hit the token limit) or
 * `content_filter`, most commonly. Whatever content it did produce is incomplete and must not be
 * used as if it were a finished answer. [finishReason] is the raw value the provider reported.
 */
class OaiIncompleteResponseException(
    val finishReason: String,
    message: String,
) : OaiResponseException(message)
