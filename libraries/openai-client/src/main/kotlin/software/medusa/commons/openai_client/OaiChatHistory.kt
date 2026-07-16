package software.medusa.commons.openai_client

import com.aallam.openai.api.chat.ChatMessage
import software.medusa.commons.openai_client.messages.OaiMessage

data class OaiChatHistory(
    val messages: List<OaiMessage>,
) {
  init {
    require(messages.isNotEmpty()) { "Completion input requires at least one message" }
  }

  internal fun toSdkMessages(): List<ChatMessage> = messages.map { it.toSdkChatMessage() }
}
