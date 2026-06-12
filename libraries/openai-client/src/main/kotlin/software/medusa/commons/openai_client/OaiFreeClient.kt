package software.medusa.commons.openai_client

import java.net.URI

interface OaiFreeClient {
  fun withTarget(
      targetBaseUrl: URI,
      targetApiKey: OaiApiKey,
  ): OaiTargetedClient
}
