plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.serialization)

  `java-library`
  `maven-publish`
}

val openAiApiKeyEnvVarName = "OPENAI_API_KEY"
val integrationTestSourceSetName = "integrationTest"

group = "software.medusa.commons"

dependencies {
  api(libs.kotlinx.coroutines.core)
  api(libs.kotlinx.schema.annotations)
  api(libs.kotlinx.schema.generator.json)

  implementation(libs.openai.kotlin)
  implementation(libs.kotlinx.serialization.json)

  runtimeOnly(libs.ktor.client.okhttp)

  testImplementation(libs.kotlin.test)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.junit.jupiter)
}

tasks.test { useJUnitPlatform() }

sourceSets {
  val main by getting

  create(integrationTestSourceSetName) {
    kotlin.srcDir("src/$integrationTestSourceSetName/kotlin")

    compileClasspath += main.output + configurations.testRuntimeClasspath.get()
    runtimeClasspath += output + compileClasspath
  }
}

val integrationTest =
    tasks.register<Test>(integrationTestSourceSetName) {
      description = "Runs integration tests."
      group = "verification"

      testClassesDirs = sourceSets[integrationTestSourceSetName].output.classesDirs
      classpath = sourceSets[integrationTestSourceSetName].runtimeClasspath
    }

java { withSourcesJar() }

kotlin {
  compilerOptions { freeCompilerArgs.set(listOf("-Xannotation-default-target=param-property")) }
}
