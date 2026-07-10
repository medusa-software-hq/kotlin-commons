package software.medusa.commons.openai_client

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OaiResponseContent_tests {
  @Test
  fun `a stop-finished response with content returns that content`() {
    val content =
        interpretResponseContent(
            hasAnyChoice = true,
            finishReason = "stop",
            messageContent = "hello",
        )

    assertEquals("hello", content)
  }

  @Test
  fun `an absent finish reason is tolerated when content is present`() {
    val content =
        interpretResponseContent(
            hasAnyChoice = true,
            finishReason = null,
            messageContent = "hello",
        )

    assertEquals("hello", content)
  }

  @Test
  fun `no choice is an empty response`() {
    assertFailsWith<OaiEmptyResponseException> {
      interpretResponseContent(hasAnyChoice = false, finishReason = null, messageContent = null)
    }
  }

  @Test
  fun `stop-finished but null content is an empty response`() {
    assertFailsWith<OaiEmptyResponseException> {
      interpretResponseContent(hasAnyChoice = true, finishReason = "stop", messageContent = null)
    }
  }

  @Test
  fun `a length finish reason is an incomplete response even with partial content`() {
    val failure =
        assertFailsWith<OaiIncompleteResponseException> {
          interpretResponseContent(
              hasAnyChoice = true,
              finishReason = "length",
              messageContent = "partial answer that got cut off",
          )
        }

    assertEquals("length", failure.finishReason)
  }

  @Test
  fun `a content_filter finish reason is an incomplete response`() {
    val failure =
        assertFailsWith<OaiIncompleteResponseException> {
          interpretResponseContent(
              hasAnyChoice = true,
              finishReason = "content_filter",
              messageContent = null,
          )
        }

    assertEquals("content_filter", failure.finishReason)
  }
}
