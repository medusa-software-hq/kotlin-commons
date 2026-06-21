plugins {
    // Apply the foojay-resolver plugin to allow automatic download of JDKs
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "commons"

include(
    ":examples:cli",
    ":libraries:git",
    ":libraries:markdown",
    ":libraries:openai-client",
    ":libraries:system",
    ":libraries:text",
    ":libraries:unix-filesystem",
    ":libraries:yaml",
)
