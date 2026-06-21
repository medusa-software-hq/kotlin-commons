package software.medusa.yaml

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml as SnakeYaml
import org.yaml.snakeyaml.constructor.SafeConstructor

/** Converts between YAML documents and [JsonElement]s, backed by SnakeYAML. */
data object Yaml {
  /** Parses [yamlString] into the [JsonElement] that mirrors its structure. */
  fun decodeFromString(
      yamlString: String,
  ): JsonElement {
    val loadedValue: Any? = newSnakeYaml().load(yamlString)

    return loadedValue.toJsonElement()
  }

  /** Renders [jsonElement] as a YAML document. */
  fun encodeToString(
      jsonElement: JsonElement,
  ): String = newSnakeYaml().dump(jsonElement.toPlainValue())

  // A SafeConstructor restricts loading to standard YAML types (maps, lists, scalars), which are
  // exactly the shapes that map onto a JsonElement.
  private fun newSnakeYaml(): SnakeYaml = SnakeYaml(SafeConstructor(LoaderOptions()))

  private fun Any?.toJsonElement(): JsonElement =
      when (this) {
        null -> JsonNull

        is Map<*, *> ->
            JsonObject(
                entries.associate { (key, value) -> key.toString() to value.toJsonElement() },
            )

        is List<*> -> JsonArray(map { element -> element.toJsonElement() })

        is Boolean -> JsonPrimitive(this)

        is Number -> JsonPrimitive(this)

        is String -> JsonPrimitive(this)

        else -> JsonPrimitive(toString())
      }

  private fun JsonElement.toPlainValue(): Any? =
      when (this) {
        is JsonNull -> null

        is JsonObject -> mapValues { (_, value) -> value.toPlainValue() }

        is JsonArray -> map { element -> element.toPlainValue() }

        is JsonPrimitive ->
            when {
              // A typed scalar (boolean/integer/floating-point) so SnakeYAML emits it unquoted.
              isString -> content
              else -> booleanOrNull ?: longOrNull ?: doubleOrNull ?: content
            }
      }
}
