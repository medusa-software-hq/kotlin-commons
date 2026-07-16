package software.medusa.commons.openai_client

import java.net.URI
import kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGenerator
import kotlinx.schema.json.JsonSchema
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

interface OaiConfiguredClient : AutoCloseable {
  @Serializable
  data class CompletionRequest(
      val input: OaiChat,
      val reasoningEffort: OaiReasoningEffort = OaiReasoningEffort.Medium,
      val maxOutputTokenCount: Int? = null,
      val temperature: Double? = null,
  )

  @Serializable
  data class Usage(
      val promptTokenCount: Int,
      val completionTokenCount: Int,
      val totalTokenCount: Int,
  )

  data class UnstructuredCompletionResponse(
      val responseText: String,
      val usage: Usage?,
  )

  data class RawStructuredCompletionResponse(
      val responseJsonElement: JsonElement,
      val usage: Usage?,
  )

  data class StructuredCompletionResponse<ResponseT : Any>(
      val responseObject: ResponseT,
      val usage: Usage?,
  )

  companion object {
    val openAiBaseUrl: URI = URI.create("https://api.openai.com/v1/")

    val openRouterBaseUrl: URI = URI.create("https://openrouter.ai/api/v1/")
  }

  suspend fun createUnstructuredCompletion(
      request: CompletionRequest,
  ): UnstructuredCompletionResponse

  suspend fun createRawStructuredCompletion(
      request: CompletionRequest,
      responseSchemaName: String,
      responseSchema: JsonSchema,
  ): RawStructuredCompletionResponse
}

// https://developers.openai.com/api/docs/guides/structured-outputs#refusals
private const val refusalFieldName = "refusal"

suspend fun <ResponseT : Any> OaiConfiguredClient.createStructuredCompletion(
    /** Completion request. */
    request: OaiConfiguredClient.CompletionRequest,
    /** Schema name (used for debugging). */
    responseSchemaName: String = "response_schema",
    /**
     * Response type serializer. Should not include a root field named `refusal`, as it's reserved.
     */
    responseSerializer: KSerializer<ResponseT>,
): OaiConfiguredClient.StructuredCompletionResponse<ResponseT> {
  val generator = SerializationClassJsonSchemaGenerator.Default
  val responseSchema = generator.generateSchema(target = responseSerializer.descriptor)

  val jsonResponse =
      createStructuredCompletion(
          request = request,
          responseSchemaName = responseSchemaName,
          responseSchema = responseSchema,
      )

  val responseJsonElement = jsonResponse.responseObject

  val responseObject =
      Json.decodeFromJsonElement(
          deserializer = responseSerializer,
          element = responseJsonElement,
      )

  return OaiConfiguredClient.StructuredCompletionResponse(
      responseObject = responseObject,
      usage = jsonResponse.usage,
  )
}

suspend fun OaiConfiguredClient.createStructuredCompletion(
    /** Completion request. */
    request: OaiConfiguredClient.CompletionRequest,
    /** Schema name (used for debugging). */
    responseSchemaName: String = "response_schema",
    /** Response JSON schema. */
    responseSchema: JsonSchema,
): OaiConfiguredClient.StructuredCompletionResponse<JsonElement> {
  val rawResponse =
      createRawStructuredCompletion(
          request = request,
          responseSchemaName = responseSchemaName,
          responseSchema = responseSchema,
      )

  val responseJsonElement = rawResponse.responseJsonElement

  val refusalElement = (responseJsonElement as JsonObject?)?.get(refusalFieldName)

  if (refusalElement != null) {
    throw IllegalStateException(
        "OpenAI refused to complete the request. Refusal reason: $refusalElement",
    )
  }

  return OaiConfiguredClient.StructuredCompletionResponse(
      responseObject = responseJsonElement,
      usage = rawResponse.usage,
  )
}
