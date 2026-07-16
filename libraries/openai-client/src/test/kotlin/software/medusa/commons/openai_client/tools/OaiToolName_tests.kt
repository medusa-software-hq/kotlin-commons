package software.medusa.commons.openai_client.tools

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OaiToolName_tests {
  @Test
  fun `accepts letters digits underscores and dashes`() {
    val name = OaiToolName("get_weather-2")

    assertEquals("get_weather-2", name.name)
  }

  @Test
  fun `accepts a name at the 64-character limit`() {
    val name = OaiToolName("a".repeat(64))

    assertEquals(64, name.name.length)
  }

  @Test
  fun `rejects an empty name`() {
    assertFailsWith<IllegalArgumentException> { OaiToolName("") }
  }

  @Test
  fun `rejects a name longer than 64 characters`() {
    assertFailsWith<IllegalArgumentException> { OaiToolName("a".repeat(65)) }
  }

  @Test
  fun `rejects a name containing a space`() {
    assertFailsWith<IllegalArgumentException> { OaiToolName("get weather") }
  }

  @Test
  fun `rejects a name containing punctuation`() {
    assertFailsWith<IllegalArgumentException> { OaiToolName("get.weather") }
  }
}
