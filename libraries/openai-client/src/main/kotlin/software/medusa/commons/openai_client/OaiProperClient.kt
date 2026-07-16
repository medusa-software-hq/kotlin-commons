package software.medusa.commons.openai_client

import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatResponseFormat
import com.aallam.openai.api.chat.JsonSchema
import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIHost
import java.net.URI
import kotlinx.schema.json.encodeToJsonObject
import kotlinx.serialization.json.Json

data object OaiProperClient : OaiFreeClient {
  override fun targeting(
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
                logging = LoggingConfig(logLevel = LogLevel.None),
            )

        return object : OaiConfiguredClient {
          override suspend fun createUnstructuredCompletion(
              request: OaiConfiguredClient.CompletionRequest,
          ): OaiConfiguredClient.UnstructuredCompletionResponse {
            val chatCompletion =
                openAi.chatCompletion(
                    ChatCompletionRequest(
                        model = model.toSdkModelId(),
                        reasoningEffort = request.reasoningEffort.toSdkEffort(),
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
  val firstChoice = choices.firstOrNull()

  return interpretResponseContent(
      hasAnyChoice = firstChoice != null,
      finishReason = firstChoice?.finishReason?.value,
      messageContent = firstChoice?.message?.content,
  )
}

private fun ChatCompletion.extractUsage(): OaiTokenUsage? {
  val sdkUsage = usage ?: return null

  return OaiTokenUsage(
      promptTokenCount = sdkUsage.promptTokens ?: 0,
      completionTokenCount = sdkUsage.completionTokens ?: 0,
      totalTokenCount = sdkUsage.totalTokens ?: 0,
  )
}
