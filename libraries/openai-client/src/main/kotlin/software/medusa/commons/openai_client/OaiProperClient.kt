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
                    reporter.reportIoError(exception)
                    return OaiResult.NetworkError
                  } catch (exception: OpenAIException) {
                    // A transport/HTTP failure without a well-formed server response to inspect.
                    reporter.reportClientError(exception)
                    return OaiResult.NetworkError
                  }

              return OaiResult.ResponseReceived(interpretCompletion(completion, reporter))
            }
          }
    }
  }
}

private val zeroTokenUsage =
    OaiTokenUsage(promptTokenCount = 0, completionTokenCount = 0, totalTokenCount = 0)

private fun interpretCompletion(
    completion: ChatCompletion,
    reporter: OaiReporter,
): OaiResponse {
  val choices = completion.choices

  val choice =
      choices.firstOrNull()
          ?: run {
            reporter.reportNoChoices()
            return OaiResponse.Corrupted
          }

  if (choices.size > 1) {
    // We still act on the first choice, but more than one is unexpected for how we issue requests.
    reporter.reportMultipleChoices(choiceCount = choices.size)
  }

  val tokenUsage =
      completion.extractTokenUsage()
          ?: run {
            reporter.reportMissingTokenUsage()
            zeroTokenUsage
          }

  return interpretChoice(choice, tokenUsage, reporter)
}

private fun interpretChoice(
    choice: ChatChoice,
    tokenUsage: OaiTokenUsage,
    reporter: OaiReporter,
): OaiResponse {
  val text = choice.message.content
  val finishReason = choice.finishReason

  return when (finishReason) {
    FinishReason.Length -> completePartial(text, OaiInterruptionReason.LengthLimit, tokenUsage)
    FinishReason.ContentFilter ->
        completePartial(text, OaiInterruptionReason.ContentFilter, tokenUsage)
    // A null finish reason is tolerated: some providers omit it on an otherwise complete response.
    null,
    FinishReason.Stop,
    FinishReason.ToolCalls,
    FinishReason.FunctionCall -> {
      val toolCalls =
          choice.message.toolCalls.orEmpty().toOaiToolCalls(reporter)
              ?: return OaiResponse.Corrupted

      if (text == null && toolCalls.isEmpty()) {
        reporter.reportEmptyResponse()
        OaiResponse.Corrupted
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
    else -> {
      reporter.reportUnknownFinishReason(finishReason.value)
      OaiResponse.Corrupted
    }
  }
}

private fun completePartial(
    text: String?,
    interruptionReason: OaiInterruptionReason,
    tokenUsage: OaiTokenUsage,
): OaiResponse.Complete =
    OaiResponse.Complete(
        generatedContent =
            OaiGeneratedContent.Partial(
                partialGeneratedText = text.orEmpty(),
                interruptionReason = interruptionReason,
            ),
        tokenUsage = tokenUsage,
    )

/**
 * Converts SDK tool calls to [OaiToolCall]s, or returns `null` after reporting the first tool call
 * that couldn't be interpreted (an invalid name or unparsable arguments).
 */
private fun List<ToolCall>.toOaiToolCalls(reporter: OaiReporter): List<OaiToolCall>? =
    filterIsInstance<ToolCall.Function>().map { toolCall ->
      val function = toolCall.function

      val toolName =
          try {
            OaiToolName(function.name)
          } catch (exception: IllegalArgumentException) {
            reporter.reportInvalidToolName(rawToolName = function.nameOrNull.orEmpty(), exception)
            return null
          }

      val passedArgument =
          try {
            Json.parseToJsonElement(function.arguments)
          } catch (exception: IllegalArgumentException) {
            // Covers both a malformed-JSON SerializationException (a subtype) and absent arguments.
            reporter.reportMalformedToolCallArguments(
                rawArguments = function.argumentsOrNull.orEmpty(),
                exception,
            )
            return null
          }

      OaiToolCall(
          toolName = toolName,
          callId = OaiToolCallId(toolCall.id.id),
          passedArgument = passedArgument,
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
