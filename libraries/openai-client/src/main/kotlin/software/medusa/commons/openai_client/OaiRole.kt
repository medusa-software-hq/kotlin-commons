package software.medusa.commons.openai_client

import kotlinx.serialization.Serializable

@Serializable
enum class OaiRole {
  System,
  User,
  Assistant,
}
