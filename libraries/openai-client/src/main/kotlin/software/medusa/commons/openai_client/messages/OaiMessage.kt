package software.medusa.commons.openai_client.messages

import com.aallam.openai.api.chat.ChatMessage

/** A single message in a chat history, discriminated by its author role. */
sealed class OaiMessage {
  internal abstract fun toSdkChatMessage(): ChatMessage
}
