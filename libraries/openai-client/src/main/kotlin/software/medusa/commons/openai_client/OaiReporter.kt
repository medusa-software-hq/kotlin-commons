package software.medusa.commons.openai_client

/**
 * Escape hatch for surfacing unexpected, critical conditions that the library folded into a coarse
 * [OaiResult]/[OaiResponse] (e.g. [OaiResult.NetworkError] or [OaiResponse.Corrupted]) rather than
 * a precise, actionable value.
 *
 * Each anomaly has its own method so implementations can categorize and count them (e.g. as
 * distinct Sentry issues) to decide whether the library should learn to handle a case first-class.
 * There are deliberately no default no-op bodies: when a new anomaly method is added, every
 * implementation should have to acknowledge it rather than silently swallow it.
 *
 * The library keeps the returned result clean and coarse; the detail is reported here so callers
 * can log it or forward it to an error tracker instead of being blind to it.
 */
interface OaiReporter {
  /** The response carried no choices at all. The call resolves to [OaiResponse.Corrupted]. */
  fun reportNoChoices()

  /**
   * The response carried more than one choice ([choiceCount] in total). The library still acts on
   * the first choice, but more than one is unexpected for the way requests are issued.
   */
  fun reportMultipleChoices(choiceCount: Int)

  /**
   * The response carried no token-usage statistics. The library proceeds with a zeroed
   * [OaiTokenUsage] rather than discarding an otherwise usable response.
   */
  fun reportMissingTokenUsage()

  /**
   * A choice finished normally but carried neither text content nor tool calls. The call resolves
   * to [OaiResponse.Corrupted] rather than fabricating empty content.
   */
  fun reportEmptyResponse()

  /**
   * A choice reported a [finishReason] this library does not recognize. The call resolves to
   * [OaiResponse.Corrupted] so unfamiliar providers surface loudly before being trusted.
   */
  fun reportUnknownFinishReason(finishReason: String)

  /**
   * A tool call named [rawToolName], which violates the tool-name constraints. The call resolves to
   * [OaiResponse.Corrupted].
   */
  fun reportInvalidToolName(rawToolName: String, cause: Throwable)

  /**
   * A tool call carried arguments ([rawArguments]) that could not be parsed as JSON. The call
   * resolves to [OaiResponse.Corrupted].
   */
  fun reportMalformedToolCallArguments(rawArguments: String, cause: Throwable)

  /** The request failed with a network/I/O error. The call resolves to [OaiResult.NetworkError]. */
  fun reportIoError(cause: Throwable)

  /**
   * The request failed with a non-API client error without a well-formed server response to
   * inspect. The call resolves to [OaiResult.NetworkError].
   */
  fun reportClientError(cause: Throwable)

  companion object {
    /** A reporter that discards every anomaly. */
    val NoOp: OaiReporter =
        object : OaiReporter {
          override fun reportNoChoices() = Unit

          override fun reportMultipleChoices(choiceCount: Int) = Unit

          override fun reportMissingTokenUsage() = Unit

          override fun reportEmptyResponse() = Unit

          override fun reportUnknownFinishReason(finishReason: String) = Unit

          override fun reportInvalidToolName(rawToolName: String, cause: Throwable) = Unit

          override fun reportMalformedToolCallArguments(rawArguments: String, cause: Throwable) =
              Unit

          override fun reportIoError(cause: Throwable) = Unit

          override fun reportClientError(cause: Throwable) = Unit
        }
  }
}
