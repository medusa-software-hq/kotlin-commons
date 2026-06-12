package software.medusa.commons.openai_client

import com.aallam.openai.api.chat.ChatMessage
import kotlinx.serialization.Serializable

@Serializable
data class OaiChat(
    val messages: List<OaiMessage>,
) {
  init {
    require(messages.isNotEmpty()) { "Completion input requires at least one message" }
  }

  internal fun toSdkMessages(): List<ChatMessage> = messages.map(OaiMessage::toSdkChatMessage)
}
