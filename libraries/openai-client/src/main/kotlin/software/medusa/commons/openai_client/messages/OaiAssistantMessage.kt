package software.medusa.commons.openai_client.messages

import com.aallam.openai.api.chat.ChatMessage
import software.medusa.commons.openai_client.tools.OaiToolCall

data class OaiAssistantMessage(
    /**
     * Assistant's message content. If the requested response format was JSON, this _should_ be a
     * valid JSON string, and it _should_ match the schema.
     */
    val content: String,
    /** Tools called by the assistant. */
    val toolCalls: List<OaiToolCall> = emptyList(),
) : OaiMessage() {
  override fun toSdkChatMessage(): ChatMessage =
      ChatMessage.Assistant(
          content = content,
          toolCalls = toolCalls.map { it.toSdkToolCall() },
      )
}
