package software.medusa.commons.openai_client

/**
 * Escape hatch for surfacing unexpected, critical conditions that the library converted into a
 * coarse [OaiResult]/[OaiResponse] (e.g. [OaiResult.NetworkError] or [OaiResponse.Corrupted])
 * rather than a precise, actionable value.
 *
 * The library keeps the returned result clean and coarse; the underlying detail is handed here so
 * callers can log it or forward it to an error tracker (e.g. Sentry) instead of being blind to it.
 */
fun interface OaiReporter {
  fun report(issue: OaiCriticalIssue)

  companion object {
    /** A reporter that discards every issue. */
    val NoOp: OaiReporter = OaiReporter {}
  }
}
