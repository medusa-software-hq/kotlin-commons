package software.medusa.commons.openai_client.tools

import com.aallam.openai.api.chat.ToolId

@JvmInline
value class OaiToolCallId(
    val id: String,
) {
  internal fun toSdkToolCallId(): ToolId = ToolId(id = id)
}
