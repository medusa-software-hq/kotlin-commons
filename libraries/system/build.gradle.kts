plugins {
  alias(libs.plugins.kotlin.jvm)

  `java-library`
  `maven-publish`
}

group = "software.medusa.commons"

version = "0.1.0-SNAPSHOT"

dependencies {
  implementation(project(":libraries:unix-filesystem"))
  implementation(libs.kotlinx.coroutines.core)

  testImplementation(libs.kotlin.test)
  testImplementation(libs.kotlinx.coroutines.test)
}

java { withSourcesJar() }
