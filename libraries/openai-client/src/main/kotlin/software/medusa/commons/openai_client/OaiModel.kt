package software.medusa.commons.openai_client

import com.aallam.openai.api.model.ModelId
import kotlinx.serialization.Serializable

/**
 * OpenAI (or OpenAI-compatible) model.
 *
 * Prices are approximate and are per 1M tokens.
 */
@Serializable
sealed class OaiModel {
  // region Western models

  // region OpenAI models

  /**
   * GPT OSS 120B
   *
   * $0.04 in (no cache), $0.18 out
   *
   * Context: 131.1K
   *
   * Notes:
   * - Wins in tasks with high input/output ratio, requiring medium intelligence?
   */
  @Serializable
  data object GptOss120b : OaiModel() {
    override val id: String = "openai/gpt-oss-120b"
  }

  /**
   * GPT 5.4 Mini
   *
   * $0.75 in (cache: $0.08 read), $4.50 out
   *
   * Context: 400K
   */
  @Serializable
  data object GptMini : OaiModel() {
    override val id: String = "openai/gpt-5.4-mini"
  }

  /**
   * GPT 5.4
   *
   * $2.50 in (cache: $0.25 read), $15.00 out
   *
   * Context: 1.05M
   *
   * Notes:
   * - Wins in tasks requiring top tier intelligence?
   */
  @Serializable
  data object GptMidi : OaiModel() {
    override val id: String = "openai/gpt-5.4"
  }

  // endregion

  // (*) For Anthropic models, cache write price is for 1 hour cache
  // region Anthropic models

  /**
   * Claude Haiku 4.6
   *
   * $1.00 in (cache: $0.10 read, $1.25 write (*)), $5.00 out
   *
   * Context: 1M
   */
  @Serializable
  data object ClaudeHaiku : OaiModel() {
    override val id: String = "anthropic/claude-haiku-4.5"
  }

  /**
   * Claude Sonnet 4.6
   *
   * $3.00 in (cache: $0.30 read, $3.75 write (*)), $15.00 out
   *
   * Context: 1M
   */
  @Serializable
  data object ClaudeSonnet : OaiModel() {
    override val id: String = "anthropic/claude-sonnet-4.6"
  }

  /**
   * Claude Opus 4.6
   *
   * $5.00 in (cache: $0.50 read, $6.25 write (*)), $25.00 out
   *
   * Context: 1M
   */
  @Serializable
  data object ClaudeOpus : OaiModel() {
    override val id: String = "anthropic/claude-opus-4.6"
  }

  // endregion

  // region Google models

  /**
   * Gemma 3 4B
   *
   * $0.05 in (no cache), $0.10 out
   *
   * Context: 131.1K
   *
   * Notes:
   * - Looses to GPT OSS 120b for tasks with high input/output ratio?
   */
  @Serializable
  data object Gemma4B : OaiModel() {
    override val id: String = "google/gemma-3-4b-it"
  }

  /**
   * Gemma 3 12B
   *
   * $0.05 in (no cache), $0.15 out
   *
   * Context: 131.1K
   *
   * Notes:
   * - Looses to GPT OSS 120b?
   */
  @Serializable
  data object Gemma12B : OaiModel() {
    override val id: String = "google/gemma-3-12b-it"
  }

  /**
   * Gemma 4 31B
   *
   * $0.12 in (no cache), $0.35 out
   *
   * Context: 262.1K
   */
  @Serializable
  data object Gemma31B : OaiModel() {
    override val id: String = "google/gemma-4-31b-it"
  }

  /**
   * Gemini 3.1 Flash Lite
   *
   * $0.10 in (cache: $0.01 read, $0.08 write), $0.40 out
   *
   * Context: 1.05M
   */
  @Serializable
  data object GeminiFlashLitePrevious : OaiModel() {
    override val id: String = "google/gemini-2.5-flash-lite"
  }

  /**
   * Gemini 3.1 Flash Lite
   *
   * $0.25 in (cache: $0.03 read, $0.08 write), $1.50 out
   *
   * Context: 1.05M
   */
  @Serializable
  data object GeminiFlashLite : OaiModel() {
    override val id: String = "google/gemini-3.1-flash-lite"
  }

  /**
   * Gemini 2.5 Flash
   *
   * $0.30 in (cache: $0.03 read, $0.08 write), $2.50 out
   *
   * Context: 1.05M
   *
   * Notes:
   * - Looses to DeepSeek V4 Flash?
   */
  @Serializable
  data object GeminiFlashPrevious : OaiModel() {
    override val id: String = "google/gemini-2.5-flash"
  }

  /**
   * Gemini 3.5 Flash
   *
   * $1.50 in (cache: $0.15 read, $0.08 write), $9.00 out
   *
   * Context: 1.05M
   */
  @Serializable
  data object GeminiFlash : OaiModel() {
    override val id: String = "google/gemini-3.5-flash"
  }

  // endregion

  // region Meta models

  /**
   * Llama 3.3 70B
   *
   * $0.10 in (no cache), $0.32 out
   *
   * Context: 131.1K
   */
  @Serializable
  data object Llama70B : OaiModel() {
    override val id: String = "meta-llama/llama-3.3-70b-instruct"
  }

  // endregion

  // endregion

  // region Chinese models

  // region DeepSeek models

  /**
   * DeepSeek V4 Flash
   *
   * $0.10 in (cache: $0.02 read), $0.20 out
   *
   * Context: 1.05M
   *
   * Notes:
   * - Wins in tasks requiring medium-to-high intelligence?
   */
  @Serializable
  data object DeepSeekFlash : OaiModel() {
    override val id: String = "deepseek/deepseek-v4-flash"
  }

  /**
   * DeepSeek V4 Pro
   *
   * $1.30 in (cache: $0.10 read), $2.60 out
   *
   * Context: 1.05M
   *
   * Notes:
   * - Wins in tasks requiring high intelligence?
   */
  @Serializable
  data object DeepSeekPro : OaiModel() {
    override val id: String = "deepseek/deepseek-v4-pro"
  }

  // endregion

  // (*) For Alibaba models, cache write price is for 5 min cache
  // region Alibaba models

  /**
   * Qwen3.6 Plus
   *
   * $0.33 in (cache: $0.03 read, $0.41 write (*), $1.95 out (< 256K tok) $1.30 in (cache: $0.13
   * read, $1.63 write (*), $3.90 out (> 256K tok)
   *
   * Context: 1M
   */
  @Serializable
  data object QwenPlus : OaiModel() {
    override val id: String = "qwen/qwen3.6-plus"
  }

  /**
   * Qwen3.7 Max
   *
   * $1.25 in (cache: $0.25 read, $1.56 write (*), $3.75 out (> 256K tok)
   *
   * Context: 1M
   */
  @Serializable
  data object QwenMax : OaiModel() {
    override val id: String = "qwen/qwen3.7-max"
  }

  // endregion

  // region Z.ai models

  /**
   * GLM 4.7 Flash
   *
   * $0.06 in (cache: $0.01 read), $0.40 out
   *
   * Context: 202.8K
   */
  @Serializable
  data object GlmFlash : OaiModel() {
    override val id: String = "z-ai/glm-4.7-flash"
  }

  // endregion

  // region MiniMax models

  /**
   * MiniMax M3
   *
   * $0.30 in (cache: $0.06 read) / $1.20 out
   *
   * Context: 1M
   */
  @Serializable
  data object MiniMax : OaiModel() {
    override val id: String = "minimax/minimax-m3"
  }

  // endregion

  // endregion

  internal fun toSdkModelId(): ModelId =
      ModelId(
          id = id,
      )

  abstract val id: String
}
