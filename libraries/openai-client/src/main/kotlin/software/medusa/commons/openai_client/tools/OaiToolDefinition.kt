package software.medusa.commons.openai_client.tools

import com.aallam.openai.api.chat.Tool
import com.aallam.openai.api.core.Parameters
import kotlinx.schema.json.JsonSchema
import kotlinx.schema.json.encodeToJsonObject

/** Definition of a tool (function) the model is allowed to call. */
data class OaiToolDefinition(
    val name: OaiToolName,
    val description: String,
    val parameterSchema: JsonSchema,
) {
  internal fun toSdkTool(): Tool =
      Tool.function(
          name = name.name,
          description = description,
          parameters = Parameters(parameterSchema.encodeToJsonObject()),
      )
}
