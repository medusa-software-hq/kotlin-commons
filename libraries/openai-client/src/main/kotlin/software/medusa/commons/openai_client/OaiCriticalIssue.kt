package software.medusa.commons.openai_client

/**
 * A critical, unexpected condition the library ran into and folded into a coarse result (e.g.
 * [OaiResult.NetworkError] or [OaiResponse.Corrupted]) rather than a precise, actionable value.
 *
 * Handed to an [OaiReporter] so callers aren't blind to it: the coarse result stays clean, while
 * the underlying detail ([message], and [cause] when there was an exception) can be logged or
 * forwarded to an error tracker.
 */
data class OaiCriticalIssue(
    val message: String,
    val cause: Throwable? = null,
)
