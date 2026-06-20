plugins {
  alias(libs.plugins.kotlin.jvm)

  `java-library`
  `maven-publish`
}

group = "software.medusa.commons"

dependencies {
  api(libs.kotlinx.serialization.json)

  implementation(libs.snakeyaml)

  testImplementation(libs.kotlin.test)
}

java { withSourcesJar() }
