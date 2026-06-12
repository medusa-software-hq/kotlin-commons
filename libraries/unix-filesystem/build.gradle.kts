plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.serialization)

  `java-library`
  `maven-publish`
}

group = "software.medusa.commons"

version = "0.1.0-SNAPSHOT"

dependencies {
  api(libs.kotlinx.io.bytestring)

  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.serialization.json)

  testImplementation(libs.kotlin.test)
  testImplementation(libs.kotlinx.coroutines.test)
}
