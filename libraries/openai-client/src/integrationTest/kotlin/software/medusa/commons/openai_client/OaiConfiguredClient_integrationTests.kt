package software.medusa.commons.openai_client

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlinx.coroutines.test.runTest
import kotlinx.schema.Description
import kotlinx.schema.Schema
import kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGenerator
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import software.medusa.commons.openai_client.messages.OaiSystemMessage
import software.medusa.commons.openai_client.messages.OaiUserMessage

class OaiProperClient_integrationTests {
  @Serializable
  @Schema
  private data class CountryInfo(
      @Description("Name of the capital city of the country") val capital: String,
      @Description("Approximate population of the country") val population: Int,
  )

  companion object {
    private const val apiKeyEnvVarName = "OPENAI_API_KEY"

    private val apiKey =
        OaiApiKey(
            content =
                System.getenv(apiKeyEnvVarName)
                    ?: error("Environment variable $apiKeyEnvVarName is not set"),
        )

    private fun buildTargetedClient(): OaiTargetedClient =
        OaiProperClient.targeting(
            targetBaseUrl = OaiFreeClient.openAiBaseUrl,
            targetApiKey = apiKey,
        )

    /** Asserts the result is a fully generated response and returns its assistant text. */
    private fun OaiResult<OaiResponse>.fullContentOrFail(): String {
      val response =
          (this as? OaiResult.ResponseReceived<OaiResponse>)?.response
              ?: fail("Expected a received response, but was: $this")
      val complete =
          response as? OaiResponse.Complete
              ?: fail("Expected a complete response, but was: $response")
      val full =
          complete.generatedContent as? OaiGeneratedContent.Full
              ?: fail("Expected fully generated content, but was: ${complete.generatedContent}")
      return full.generatedMessage.content
    }
  }

  @Test
  fun test_unstructuredCompletion() = runTest {
    val client = buildTargetedClient().configured(model = OaiModel.GptMidi)

    val result =
        client.completeChat(
            chatHistory =
                OaiChatHistory(
                    messages =
                        listOf(
                            OaiSystemMessage(content = "You are a Star Wars meme expert."),
                            OaiUserMessage(content = "Hello there!"),
                        ),
                ),
        )

    assertEquals(
        actual = result.fullContentOrFail(),
        expected = "General Kenobi!",
    )
  }

  @Test
  fun test_structuredCompletion() = runTest {
    val schema =
        SerializationClassJsonSchemaGenerator.Default.generateSchema(
            target = CountryInfo.serializer().descriptor,
        )

    val client =
        buildTargetedClient()
            .configured(
                model = OaiModel.GptMini,
                responseFormat = OaiResponseFormat.Json(name = "country_info", schema = schema),
            )

    val result =
        client.completeChat(
            chatHistory =
                OaiChatHistory(
                    messages =
                        listOf(
                            OaiUserMessage(content = "Provide information about France"),
                        ),
                ),
        )

    val countryInfo =
        Json.decodeFromString(
            deserializer = CountryInfo.serializer(),
            string = result.fullContentOrFail(),
        )

    assertEquals(
        expected = "Paris",
        actual = countryInfo.capital,
    )

    assertTrue(
        countryInfo.population in 65_000_000..70_000_000,
    )
  }
}
