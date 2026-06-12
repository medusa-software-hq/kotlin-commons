package software.medusa.commons.openai_client

import com.aallam.openai.api.model.ModelId
import kotlinx.serialization.Serializable

/** OpenAI (or OpenAI-compatible) model. */
@Serializable
sealed class OaiModel {
  @Serializable
  data object GptMini : OaiModel() {
    override val id: String = "gpt-5.4-mini"
  }

  @Serializable
  data object GptMidi : OaiModel() {
    override val id: String = "gpt-5.4"
  }

  @Serializable
  data object GptOss120b : OaiModel() {
    override val id: String = "openai/gpt-oss-120b"
  }

  @Serializable
  data object Gemma4B : OaiModel() {
    override val id: String = "google/gemma-3-4b-it"
  }

  @Serializable
  data object Gemma12B : OaiModel() {
    override val id: String = "google/gemma-3-12b-it"
  }

  @Serializable
  data object Gemma31B : OaiModel() {
    override val id: String = "google/gemma-4-31b-it"
  }

  // 2x cheaper than 3.1 Flash Lite
  @Serializable
  data object GeminiFlashLitePrevious : OaiModel() {
    override val id: String = "google/gemini-2.5-flash-lite"
  }

  @Serializable
  data object GeminiFlashLite : OaiModel() {
    override val id: String = "google/gemini-3.1-flash-lite"
  }

  // 5x cheaper on input than 3.5 Flash
  @Serializable
  data object GeminiFlashPrevious : OaiModel() {
    override val id: String = "google/gemini-2.5-flash"
  }

  @Serializable
  data object GeminiFlash : OaiModel() {
    override val id: String = "google/gemini-3.5-flash"
  }

  @Serializable
  data object DeepSeekFlash : OaiModel() {
    override val id: String = "deepseek/deepseek-v4-flash"
  }

  @Serializable
  data object DeepSeekPro : OaiModel() {
    override val id: String = "deepseek/deepseek-v4-pro"
  }

  @Serializable
  data object MiniMax : OaiModel() {
    override val id: String = "minimax/minimax-m2.7"
  }

  internal fun toSdkModelId(): ModelId =
      ModelId(
          id = id,
      )

  abstract val id: String
}
