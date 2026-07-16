package software.medusa.commons.openai_client

data class OaiTokenUsage(
    val promptTokenCount: Int,
    val completionTokenCount: Int,
    val totalTokenCount: Int,
)
