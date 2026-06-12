package software.medusa.commons.openai_client

import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatResponseFormat
import com.aallam.openai.api.chat.JsonSchema
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIHost
import java.net.URI
import kotlinx.schema.json.encodeToJsonObject
import kotlinx.serialization.json.Json

data object OaiProperClient : OaiFreeClient {
  override fun withTarget(
      targetBaseUrl: URI,
      targetApiKey: OaiApiKey,
  ): OaiTargetedClient {
    val host =
        OpenAIHost(
            baseUrl = targetBaseUrl.toASCIIString(),
        )

    return object : OaiTargetedClient {
      override fun withModel(
          model: OaiModel,
      ): OaiConfiguredClient {
        val openAi =
            OpenAI(
                token = targetApiKey.content,
                host = host,
            )

        return object : OaiConfiguredClient {
          override suspend fun createUnstructuredCompletion(
              request: OaiConfiguredClient.CompletionRequest,
          ): OaiConfiguredClient.UnstructuredCompletionResponse {
            val chatCompletion =
                openAi.chatCompletion(
                    ChatCompletionRequest(
                        model = model.toSdkModelId(),
                        messages = request.input.toSdkMessages(),
                        maxCompletionTokens = request.maxOutputTokenCount,
                        temperature = request.temperature,
                    ),
                )

            val responseText = chatCompletion.extractResponseContent()

            return OaiConfiguredClient.UnstructuredCompletionResponse(
                responseText = responseText,
                usage = chatCompletion.extractUsage(),
            )
          }

          override suspend fun createRawStructuredCompletion(
              request: OaiConfiguredClient.CompletionRequest,
              responseSchemaName: String,
              responseSchema: kotlinx.schema.json.JsonSchema,
          ): OaiConfiguredClient.RawStructuredCompletionResponse {
            val responseSchemaJsonObject = responseSchema.encodeToJsonObject()

            val chatCompletion =
                openAi.chatCompletion(
                    ChatCompletionRequest(
                        model = model.toSdkModelId(),
                        messages = request.input.toSdkMessages(),
                        responseFormat =
                            ChatResponseFormat.jsonSchema(
                                JsonSchema(
                                    name = responseSchemaName,
                                    schema = responseSchemaJsonObject,
                                ),
                            ),
                        maxCompletionTokens = request.maxOutputTokenCount,
                        temperature = request.temperature,
                    ),
                )

            val responseJsonText = chatCompletion.extractResponseContent()

            val responseJsonElement =
                Json.parseToJsonElement(
                    string = responseJsonText,
                )

            return OaiConfiguredClient.RawStructuredCompletionResponse(
                responseJsonElement = responseJsonElement,
                usage = chatCompletion.extractUsage(),
            )
          }

          override fun close() {
            openAi.close()
          }
        }
      }
    }
  }
}

private fun ChatCompletion.extractResponseContent(): String {
  val firstChoice =
      choices.firstOrNull()
          ?: throw IllegalStateException("OpenAI response did not contain any choices")

  val message = firstChoice.message

  val messageContent =
      message.content
          ?: throw IllegalStateException("OpenAI response choice did not contain text content")

  return messageContent
}

private fun ChatCompletion.extractUsage(): OaiConfiguredClient.Usage? {
  val sdkUsage = usage ?: return null

  return OaiConfiguredClient.Usage(
      promptTokenCount = sdkUsage.promptTokens ?: 0,
      completionTokenCount = sdkUsage.completionTokens ?: 0,
      totalTokenCount = sdkUsage.totalTokens ?: 0,
  )
}
