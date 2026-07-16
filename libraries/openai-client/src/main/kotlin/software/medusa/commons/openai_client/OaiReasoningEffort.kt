package software.medusa.commons.openai_client

import com.aallam.openai.api.chat.Effort

enum class OaiReasoningEffort {
  None,
  Minimal,
  Low,
  Medium,
  High,
  ExtraHigh;

  internal fun toSdkEffort(): Effort =
      when (this) {
        None -> Effort("none")
        Minimal -> Effort("minimal")
        Low -> Effort("low")
        Medium -> Effort("medium")
        High -> Effort("high")
        ExtraHigh -> Effort("xhigh")
      }
}
