package software.medusa.commons.openai_client

import com.aallam.openai.api.chat.ChatMessage
import kotlinx.serialization.Serializable

@Serializable
data class OaiMessage(
    val role: OaiRole,
    val text: String,
    val name: String? = null,
) {
  internal fun toSdkChatMessage(): ChatMessage =
      when (role) {
        OaiRole.System -> ChatMessage.System(content = text, name = name)
        OaiRole.User -> ChatMessage.User(content = text, name = name)
        OaiRole.Assistant -> ChatMessage.Assistant(content = text, name = name)
      }
}
