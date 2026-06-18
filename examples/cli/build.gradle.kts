plugins {
  application

  alias(libs.plugins.kotlin.jvm)
}

dependencies {
  implementation(project(":libraries:git"))

  implementation(libs.clikt)
}

application {
  mainClass = "software.medusa.git.cli_tool.MainKt"

  applicationDefaultJvmArgs =
      listOf(
          // JNA loads native libraries via System.load; recent JDKs require explicit native-access
          // opt-in.
          "--enable-native-access=ALL-UNNAMED",
      )
}
