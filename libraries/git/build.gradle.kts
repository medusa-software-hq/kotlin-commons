plugins {
  alias(libs.plugins.kotlin.jvm)

  `java-library`
  `maven-publish`
}

group = "software.medusa.commons"

java { withSourcesJar() }

dependencies {
  api(libs.kotlinx.io.bytestring)

  implementation(project(":libraries:unix-filesystem"))

  implementation(libs.jgit)
  implementation(libs.kotlinx.coroutines.core)

  testImplementation(libs.kotlin.test)
  testImplementation(libs.kotlinx.coroutines.test)
}
