import org.gradle.api.publish.maven.MavenPublication

plugins {
  alias(libs.plugins.kotlin.jvm)

  `java-library`
  `maven-publish`
}

group = "software.medusa"

version = "0.1.0-SNAPSHOT"

val cloudsmithUsernameEnvVarName = "CLOUDSMITH_USERNAME"
val cloudsmithPasswordEnvVarName = "CLOUDSMITH_PASSWORD"

dependencies { testImplementation(libs.kotlin.test) }

publishing {
  publications {
    create<MavenPublication>("mavenJava") {
      from(components["java"])

      artifactId = "git"
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
