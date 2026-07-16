package software.medusa.commons.openai_client

import software.medusa.commons.openai_client.messages.OaiAssistantMessage

/** The content a model produced for a single response. */
sealed class OaiGeneratedContent {
  /** The model finished normally; [generatedMessage] is the complete assistant message. */
  data class Full(
      val generatedMessage: OaiAssistantMessage,
  ) : OaiGeneratedContent()

  /**
   * The model was interrupted before finishing. [partialGeneratedText] is whatever it produced so
   * far and must not be treated as a finished answer; [interruptionReason] says why it stopped.
   */
  data class Partial(
      val partialGeneratedText: String,
      val interruptionReason: OaiInterruptionReason,
  ) : OaiGeneratedContent()
}
