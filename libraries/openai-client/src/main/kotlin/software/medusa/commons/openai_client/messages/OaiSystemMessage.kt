package software.medusa.commons.openai_client.messages

import com.aallam.openai.api.chat.ChatMessage

data class OaiSystemMessage(
    val content: String,
) : OaiMessage() {
  override fun toSdkChatMessage(): ChatMessage = ChatMessage.System(content = content)
}
