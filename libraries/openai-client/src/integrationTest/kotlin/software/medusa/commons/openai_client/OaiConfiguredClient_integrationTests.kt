package software.medusa.commons.openai_client

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.schema.Description
import kotlinx.schema.Schema
import kotlinx.serialization.Serializable

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
        OaiProperClient.withTarget(
            targetBaseUrl = OaiConfiguredClient.openAiBaseUrl,
            targetApiKey = apiKey,
        )
  }

  @Test
  fun test_createUnstructuredCompletion() = runTest {
    val client = buildTargetedClient()

    val response =
        client
            .withModel(
                model = OaiModel.GptMidi,
            )
            .createUnstructuredCompletion(
                request =
                    OaiConfiguredClient.CompletionRequest(
                        input =
                            OaiChatHistory(
                                messages =
                                    listOf(
                                        OaiMessage(
                                            role = OaiRole.System,
                                            text = "You are a Star Wars meme expert.",
                                        ),
                                        OaiMessage(
                                            role = OaiRole.User,
                                            text = "Hello there!",
                                        ),
                                    ),
                            ),
                    ),
            )

    assertEquals(
        actual = "General Kenobi!",
        expected = response.responseText,
    )
  }

  @Test
  fun test_createStructuredCompletion() = runTest {
    val client = buildTargetedClient()

    val response =
        client
            .withModel(
                model = OaiModel.GptMini,
            )
            .createStructuredCompletion(
                request =
                    OaiConfiguredClient.CompletionRequest(
                        input =
                            OaiChatHistory(
                                messages =
                                    listOf(
                                        OaiMessage(
                                            role = OaiRole.User,
                                            text = "Provide information about France",
                                        ),
                                    ),
                            ),
                    ),
                responseSerializer = CountryInfo.serializer(),
            )

    val responseObject = response.responseObject

    assertEquals(
        expected = "Paris",
        actual = responseObject.capital,
    )

    assertTrue(
        responseObject.population in 65_000_000..70_000_000,
    )
  }
}
