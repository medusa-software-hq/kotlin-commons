package software.medusa.commons.openai_client

data class OaiInferenceParams(
    val reasoningEffort: OaiReasoningEffort = OaiReasoningEffort.Medium,
    val maxOutputTokenCount: Int? = null,
    val temperature: Double? = null,
)
