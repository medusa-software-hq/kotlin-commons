package software.medusa.commons.openai_client.tools

import com.aallam.openai.api.chat.FunctionCall
import com.aallam.openai.api.chat.ToolCall
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/** A single tool (function) call requested by the model. */
data class OaiToolCall(
    val toolName: OaiToolName,
    val callId: OaiToolCallId,
    val passedArgument: JsonElement,
) {
  internal fun toSdkToolCall(): ToolCall.Function =
      ToolCall.Function(
          id = callId.toSdkToolCallId(),
          function =
              FunctionCall(
                  nameOrNull = toolName.name,
                  argumentsOrNull = Json.encodeToString(passedArgument),
              ),
      )
}
