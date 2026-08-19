plugins {
  alias(libs.plugins.kotlin.jvm)

  `java-library`
  `maven-publish`
}

group = "software.medusa.commons"

dependencies {
  implementation(project(":libraries:unix-filesystem"))
  // The scope API exposes Flow, so consumers need it on their compile classpath.
  api(libs.kotlinx.coroutines.core)

  testImplementation(libs.kotlin.test)
  testImplementation(libs.kotlinx.coroutines.test)
}

java { withSourcesJar() }
