package software.medusa.commons.openai_client

import java.net.URI

interface OaiFreeClient {
  fun targeting(
      targetBaseUrl: URI,
      targetApiKey: OaiApiKey,
      reporter: OaiReporter = OaiReporter.NoOp,
  ): OaiTargetedClient

  companion object {
    val openAiBaseUrl: URI = URI.create("https://api.openai.com/v1/")

    val openRouterBaseUrl: URI = URI.create("https://openrouter.ai/api/v1/")
  }
}
