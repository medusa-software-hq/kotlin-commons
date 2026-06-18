package software.medusa.commons.system

import java.io.File
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isExecutable
import kotlin.io.path.isRegularFile

/** Represents a prepared executable that can be handed to [SysProcessSpawner]. */
@JvmInline
value class SysExecutableHandle(
    val path: Path,
) {
  companion object {
    /** Resolves [executablePath] into a validated executable handle. */
    fun resolve(executablePath: Path): SysExecutableHandle {
      require(executablePath.isAbsolute) {
        "Expected an absolute path to an executable, but got: $executablePath"
      }
      require(executablePath.isRegularFile()) {
        "Expected a regular file for an executable, but got: $executablePath"
      }
      require(executablePath.isExecutable()) {
        "Expected an executable file, but got: $executablePath"
      }

      return SysExecutableHandle(path = executablePath)
    }

    /** Locates [commandName] using the current `PATH`. */
    fun locate(commandName: String): SysExecutableHandle {
      val pathEntries =
          System.getenv("PATH")?.split(File.pathSeparatorChar)
              ?: error("PATH environment variable is not set")

      return pathEntries.firstNotNullOfOrNull { binDirectoryPathString ->
        val candidatePath = Path.of(binDirectoryPathString).resolve(commandName)

        when {
          candidatePath.exists() -> resolve(executablePath = candidatePath)
          else -> null
        }
      }
          ?: throw IllegalArgumentException(
              "Command '$commandName' cannot be found in the system PATH"
          )
    }
  }
}
