package software.medusa.commons.openai_client

import com.aallam.openai.api.chat.ChatChoice
import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ToolCall
import com.aallam.openai.api.core.FinishReason
import com.aallam.openai.api.exception.OpenAIAPIException
import com.aallam.openai.api.exception.OpenAIException
import com.aallam.openai.api.exception.OpenAIIOException
import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIHost
import com.linecorp.armeria.common.HttpStatus
import java.net.URI
import kotlinx.serialization.json.Json
import software.medusa.commons.openai_client.messages.OaiAssistantMessage
import software.medusa.commons.openai_client.tools.OaiToolCall
import software.medusa.commons.openai_client.tools.OaiToolCallId
import software.medusa.commons.openai_client.tools.OaiToolDefinition
import software.medusa.commons.openai_client.tools.OaiToolName

data object OaiProperClient : OaiFreeClient {
  override fun targeting(
      targetBaseUrl: URI,
      targetApiKey: OaiApiKey,
      reporter: OaiReporter,
  ): OaiTargetedClient {
    val openAi =
        OpenAI(
            token = targetApiKey.content,
            host = OpenAIHost(baseUrl = targetBaseUrl.toASCIIString()),
            logging = LoggingConfig(logLevel = LogLevel.None),
        )

    return object : OaiTargetedClient {
      override fun configured(
          model: OaiModel,
          responseFormat: OaiResponseFormat,
          toolDefinitions: List<OaiToolDefinition>,
      ): OaiConfiguredClient =
          object : OaiConfiguredClient {
            override suspend fun completeChat(
                chatHistory: OaiChatHistory,
                inferenceParams: OaiInferenceParams,
            ): OaiResult<OaiResponse> {
              val request =
                  ChatCompletionRequest(
                      model = model.toSdkModelId(),
                      messages = chatHistory.toSdkMessages(),
                      reasoningEffort = inferenceParams.reasoningEffort.toSdkEffort(),
                      maxCompletionTokens = inferenceParams.maxOutputTokenCount,
                      temperature = inferenceParams.temperature,
                      responseFormat = responseFormat.toSdkChatResponseFormat(),
                      tools = toolDefinitions.map { it.toSdkTool() }.ifEmpty { null },
                  )

              val completion =
                  try {
                    openAi.chatCompletion(request)
                  } catch (exception: OpenAIAPIException) {
                    // The server responded with an HTTP error status; the response was transported.
                    return OaiResult.ResponseReceived(
                        OaiResponse.Error(
                            status = HttpStatus.valueOf(exception.statusCode),
                            message = exception.message ?: "OpenAI API error",
                        ),
                    )
                  } catch (exception: OpenAIIOException) {
                    reporter.report(
                        OaiCriticalIssue("Network I/O error during chat completion", exception),
                    )
                    return OaiResult.NetworkError
                  } catch (exception: OpenAIException) {
                    // A transport/HTTP failure without a well-formed server response to inspect.
                    reporter.report(
                        OaiCriticalIssue("OpenAI client error during chat completion", exception),
                    )
                    return OaiResult.NetworkError
                  }

              return OaiResult.ResponseReceived(interpretCompletion(completion, reporter))
            }
          }
    }
  }
}

private fun interpretCompletion(
    completion: ChatCompletion,
    reporter: OaiReporter,
): OaiResponse {
  val choice =
      completion.choices.firstOrNull()
          ?: return reporter.corrupted("OpenAI response contained no choices")

  val tokenUsage =
      completion.extractTokenUsage()
          ?: return reporter.corrupted("OpenAI response did not include token usage")

  return runCatching { interpretChoice(choice, tokenUsage, reporter) }
      .getOrElse { throwable ->
        reporter.corrupted("Failed to interpret the OpenAI response", throwable)
      }
}

private fun interpretChoice(
    choice: ChatChoice,
    tokenUsage: OaiTokenUsage,
    reporter: OaiReporter,
): OaiResponse {
  val text = choice.message.content

  return when (choice.finishReason) {
    FinishReason.Length ->
        OaiResponse.Complete(
            generatedContent =
                OaiGeneratedContent.Partial(
                    partialGeneratedText = text.orEmpty(),
                    interruptionReason = OaiInterruptionReason.LengthLimit,
                ),
            tokenUsage = tokenUsage,
        )
    FinishReason.ContentFilter ->
        OaiResponse.Complete(
            generatedContent =
                OaiGeneratedContent.Partial(
                    partialGeneratedText = text.orEmpty(),
                    interruptionReason = OaiInterruptionReason.ContentFilter,
                ),
            tokenUsage = tokenUsage,
        )
    // A null finish reason is tolerated: some providers omit it on an otherwise complete response.
    null,
    FinishReason.Stop,
    FinishReason.ToolCalls,
    FinishReason.FunctionCall -> {
      val toolCalls = choice.message.toolCalls.orEmpty().toOaiToolCalls()

      if (text == null && toolCalls.isEmpty()) {
        reporter.corrupted("OpenAI response choice had neither content nor tool calls")
      } else {
        OaiResponse.Complete(
            generatedContent =
                OaiGeneratedContent.Full(
                    generatedMessage =
                        OaiAssistantMessage(content = text.orEmpty(), toolCalls = toolCalls),
                ),
            tokenUsage = tokenUsage,
        )
      }
    }
    else ->
        reporter.corrupted(
            "OpenAI reported an unrecognized finish reason: ${choice.finishReason?.value}",
        )
  }
}

private fun List<ToolCall>.toOaiToolCalls(): List<OaiToolCall> =
    filterIsInstance<ToolCall.Function>().map { toolCall ->
      OaiToolCall(
          toolName = OaiToolName(toolCall.function.name),
          callId = OaiToolCallId(toolCall.id.id),
          passedArgument = Json.parseToJsonElement(toolCall.function.arguments),
      )
    }

private fun ChatCompletion.extractTokenUsage(): OaiTokenUsage? {
  val sdkUsage = usage ?: return null

  return OaiTokenUsage(
      promptTokenCount = sdkUsage.promptTokens ?: 0,
      completionTokenCount = sdkUsage.completionTokens ?: 0,
      totalTokenCount = sdkUsage.totalTokens ?: 0,
  )
}

private fun OaiReporter.corrupted(
    message: String,
    cause: Throwable? = null,
): OaiResponse.Corrupted {
  report(OaiCriticalIssue(message = message, cause = cause))
  return OaiResponse.Corrupted
}
