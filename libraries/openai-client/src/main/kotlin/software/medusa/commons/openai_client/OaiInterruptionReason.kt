package software.medusa.commons.openai_client

/** Why the model stopped generating before producing a complete response. */
enum class OaiInterruptionReason {
  /** The output hit the token limit (`length` finish reason). */
  LengthLimit,

  /** The output was cut off by the content filter (`content_filter` finish reason). */
  ContentFilter,
}
