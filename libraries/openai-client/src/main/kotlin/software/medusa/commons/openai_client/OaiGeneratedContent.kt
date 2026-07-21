package software.medusa.commons.openai_client

import software.medusa.commons.openai_client.messages.OaiAssistantMessage

/** The content a model produced for a single response. */
sealed class OaiGeneratedContent {
  /** The model finished normally; [generatedMessage] is the complete assistant message. */
  data class Full(
      val generatedMessage: OaiAssistantMessage,
  ) : OaiGeneratedContent()

  /**
   * The model was interrupted before finishing; [interruptionReason] says why it stopped. Nothing
   * here may be treated as a finished answer.
   */
  data class Partial(
      /**
       * The answer text produced so far, or `null` if the model produced none. A `null` here likely
       * indicates the model never reached the proper content-generation phase before it was
       * interrupted (e.g. it hit the token limit while still reasoning).
       */
      val partialGeneratedText: String?,
      /**
       * The model's reasoning content, if the provider returned any. This may itself be complete or
       * cut off: reasoning is produced before [partialGeneratedText], so an interruption can land
       * within the reasoning phase or after it. `null` means no reasoning content was returned.
       */
      val reasoningText: String?,
      val interruptionReason: OaiInterruptionReason,
  ) : OaiGeneratedContent()
}
