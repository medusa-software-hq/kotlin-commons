package software.medusa.yaml

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json

class Yaml_tests {
  @Test
  fun `decodes a YAML mapping into a JsonElement`() {
    val yamlString =
        """
        name: flow
        count: 3
        enabled: true
        tags:
          - a
          - b
        """
            .trimIndent()

    val decoded = Yaml.decodeFromString(yamlString)

    val expected =
        Json.parseToJsonElement(
            """{"name":"flow","count":3,"enabled":true,"tags":["a","b"]}""",
        )

    assertEquals(expected, decoded)
  }

  @Test
  fun `round-trips a JsonElement through encode and decode`() {
    val element =
        Json.parseToJsonElement(
            """{"name":"flow","count":3,"ratio":2.5,"nested":{"flag":false},"items":["x",null]}""",
        )

    val yamlString = Yaml.encodeToString(element)

    assertEquals(element, Yaml.decodeFromString(yamlString))
  }
}
