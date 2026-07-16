package software.medusa.commons.openai_client.tools

import kotlinx.schema.json.JsonSchema

/** Definition of a tool (function) the model is allowed to call. */
data class OaiToolDefinition(
    val name: OaiToolName,
    val description: String,
    val parameterSchema: JsonSchema,
)
