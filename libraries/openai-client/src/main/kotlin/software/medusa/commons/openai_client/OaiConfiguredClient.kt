package software.medusa.commons.openai_client

/**
 * A client bound to a target, model, response format, and tool set. All that's left to vary between
 * calls is the chat history and the inference parameters.
 */
interface OaiConfiguredClient {
  suspend fun completeChat(
      chatHistory: OaiChatHistory,
      inferenceParams: OaiInferenceParams = OaiInferenceParams(),
  ): OaiResult<OaiResponse>
}
