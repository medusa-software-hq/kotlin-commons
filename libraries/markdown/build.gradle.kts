plugins {
  alias(libs.plugins.kotlin.jvm)

  `java-library`
  `maven-publish`
}

group = "software.medusa.commons"

version = "0.1.0-SNAPSHOT"

dependencies {
  implementation(libs.commonmark)

  testImplementation(libs.kotlin.test)
  testImplementation(libs.junit.jupiter)
}

tasks.test { useJUnitPlatform() }

java {
  withSourcesJar()
}
