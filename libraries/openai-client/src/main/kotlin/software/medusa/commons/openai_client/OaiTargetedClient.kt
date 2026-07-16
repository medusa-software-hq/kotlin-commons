package software.medusa.commons.openai_client

import software.medusa.commons.openai_client.tools.OaiToolDefinition

interface OaiTargetedClient {
  fun configured(
      model: OaiModel,
      responseFormat: OaiResponseFormat = OaiResponseFormat.Text,
      toolDefinitions: List<OaiToolDefinition> = emptyList(),
  ): OaiConfiguredClient
}
