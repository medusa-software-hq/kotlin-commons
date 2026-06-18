import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository

plugins {
  alias(libs.plugins.versionCatalogUpdate)

  alias(libs.plugins.kotlin.jvm) apply false
  alias(libs.plugins.ktfmt) apply false
  alias(libs.plugins.detekt) apply false
}

val kotlinJvmPluginId = libs.plugins.kotlin.jvm.get().pluginId
val detektPluginId = libs.plugins.detekt.get().pluginId
val ktfmtPluginId = libs.plugins.ktfmt.get().pluginId

// Java 21 is the current broadly adopted LTS
val usedJavaVersion = 21

allprojects {
  repositories {
    mavenCentral()
  }

  version = rootProject.version
}

val cloudsmithUsernameEnvVarName = "CLOUDSMITH_USERNAME"
val cloudsmithPasswordEnvVarName = "CLOUDSMITH_PASSWORD"

subprojects {
  pluginManager.withPlugin("maven-publish") {
    extensions.configure<PublishingExtension> {
      publications {
        create<MavenPublication>("mavenJava") {
          from(components["java"])
        }
      }

      repositories {
        maven {
          name = "cloudsmithPublicUpload"
          url = uri("https://maven.cloudsmith.io/medusa-software/public/")

          // Cloudsmith username/password pair can be either:
          // - username = <Cloudsmith username> / password = <Cloudsmith Personal API Key>
          // - username = 'token' / password = <Cloudsmith Entitlement Token>
          credentials {
            username = System.getenv(cloudsmithUsernameEnvVarName)
            password = System.getenv(cloudsmithPasswordEnvVarName)
          }
        }
      }
    }

    gradle.taskGraph.whenReady {
      if (allTasks.any { it is PublishToMavenRepository }) {
        require(!System.getenv(cloudsmithUsernameEnvVarName).isNullOrBlank()) {
          "$cloudsmithUsernameEnvVarName must be set when publishing."
        }

        require(!System.getenv(cloudsmithPasswordEnvVarName).isNullOrBlank()) {
          "$cloudsmithPasswordEnvVarName must be set when publishing."
        }
      }
    }
  }

  // Configure Kotlin/JVM modules.
  pluginManager.withPlugin(kotlinJvmPluginId) {
    // Apply Kotlin formatting and static analysis plugins.
    pluginManager.apply(ktfmtPluginId)
    pluginManager.apply(detektPluginId)

    tasks.named("check") {
      // Run formatting checks as part of the standard verification lifecycle.
      dependsOn(tasks.named("ktfmtCheck"))
    }

    extensions.configure<JavaPluginExtension> {
      toolchain {
        // Use a consistent Java toolchain version across local and CI builds.
        languageVersion = JavaLanguageVersion.of(usedJavaVersion)
      }
    }

    extensions.configure<DetektExtension> {
      buildUponDefaultConfig = true
    }

    tasks.withType<JavaCompile>().configureEach {
      // Preserve parameter names in bytecode for runtime reflection.
      options.compilerArgs.add("-parameters")
    }
  }
}
