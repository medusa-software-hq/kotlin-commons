package software.medusa.commons.openai_client.messages

import com.aallam.openai.api.chat.ChatMessage

data class OaiUserMessage(
    val content: String,
    val name: OaiUserName? = null,
) : OaiMessage() {
  override fun toSdkChatMessage(): ChatMessage =
      ChatMessage.User(content = content, name = name?.name)
}
