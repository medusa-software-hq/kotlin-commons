package software.medusa.commons.openai_client

import com.aallam.openai.api.chat.ChatResponseFormat
import kotlinx.schema.json.JsonSchema
import kotlinx.schema.json.encodeToJsonObject

sealed class OaiResponseFormat {
  data object Text : OaiResponseFormat() {
    override fun toSdkChatResponseFormat(): ChatResponseFormat = ChatResponseFormat.Text
  }

  data class Json(
      val name: String,
      val schema: JsonSchema,
  ) : OaiResponseFormat() {
    override fun toSdkChatResponseFormat(): ChatResponseFormat =
        ChatResponseFormat.jsonSchema(
            schema =
                com.aallam.openai.api.chat.JsonSchema(
                    name = name,
                    schema = schema.encodeToJsonObject(),
                ),
        )
  }

  internal abstract fun toSdkChatResponseFormat(): ChatResponseFormat
}
