package software.medusa.commons.openai_client

import com.linecorp.armeria.common.HttpStatus

/** A response from the server. */
sealed class OaiResponse {
  /** A response that's successful from the server's perspective. */
  sealed class Success : OaiResponse()

  /**
   * A technically complete response from the server, including the token usage statistics. It's not
   * guaranteed that the model stopped generating tokens without interruption or that the textual
   * response meets any specific expectations (see [OaiGeneratedContent]).
   */
  data class Complete(
      val generatedContent: OaiGeneratedContent,
      val tokenUsage: OaiTokenUsage,
  ) : Success()

  /**
   * A response that was received, but was technically incomplete or invalid according to this
   * library. May indicate a bug or malfunction on the provider's side. The underlying detail is
   * surfaced through the configured [OaiReporter] rather than this value.
   */
  data object Corrupted : Success()

  /** A response that's an error from the server's perspective. */
  data class Error(
      val status: HttpStatus,
      val message: String,
  ) : OaiResponse()
}
