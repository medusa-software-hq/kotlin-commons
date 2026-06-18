plugins {
  alias(libs.plugins.kotlin.jvm)

  `java-library`
  `maven-publish`
}

group = "software.medusa.commons"

version = "0.1.0-SNAPSHOT"

dependencies {
  api(libs.kotlinx.io.bytestring)

  testImplementation(libs.kotlin.test)
}

java { withSourcesJar() }
