package software.medusa.commons.openai_client

interface OaiTargetedClient {
  fun withModel(
      model: OaiModel,
  ): OaiConfiguredClient
}
