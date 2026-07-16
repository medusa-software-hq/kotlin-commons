package software.medusa.commons.openai_client.messages

import com.aallam.openai.api.chat.ChatMessage
import software.medusa.commons.openai_client.tools.OaiToolCallId

data class OaiToolOutputMessage(
    val callId: OaiToolCallId,
    val output: String,
) : OaiMessage() {
  override fun toSdkChatMessage(): ChatMessage =
      ChatMessage.Tool(
          toolCallId = callId.toSdkToolCallId(),
          content = output,
      )
}
