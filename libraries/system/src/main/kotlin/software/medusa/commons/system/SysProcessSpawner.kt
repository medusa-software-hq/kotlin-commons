package software.medusa.commons.system

import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Coordinates spawning native processes for tooling that relies on concrete executables. */
class SysProcessSpawner(
    runtime: Runtime = Runtime.getRuntime(),
) {
  private val childProcesses = ConcurrentHashMap.newKeySet<Process>()

  init {
    runtime.addShutdownHook(Thread { destroyAll() })
  }

  /** Launches [executable] using the provided [arguments] and captures the resulting outputs. */
  suspend fun spawn(
      executable: SysExecutableHandle,
      workingDirectory: Path? = null,
      arguments: List<String> = emptyList(),
      environment: Map<String, String> = System.getenv(),
  ): SysProcessOutcome {
    return withContext(Dispatchers.IO) {
      val argv = listOf(executable.path.toString()) + arguments
      val processBuilder =
          ProcessBuilder(argv).redirectErrorStream(false).apply {
            if (workingDirectory != null) {
              directory(workingDirectory.toFile())
            }

            environment().clear()
            environment().putAll(environment)
          }

      val process = processBuilder.start()
      childProcesses.add(process)
      process.onExit().thenRun { childProcesses.remove(process) }

      val standardOutput = process.inputStream.bufferedReader().use { it.readText() }
      val errorOutput = process.errorStream.bufferedReader().use { it.readText() }
      val exitCode = process.waitFor()

      SysProcessOutcome(
          exitCode = exitCode,
          standardOutput = standardOutput,
          errorOutput = errorOutput,
      )
    }
  }

  private fun destroyAll(
      timeout: Duration = 3.seconds,
      graceMillis: Long = 3_000,
  ) {
    childProcesses.forEach { childProcess -> runCatching { childProcess.destroy() } }

    val deadlineMillis = System.currentTimeMillis() + graceMillis

    childProcesses.forEach { childProcess ->
      val remainingMillis = deadlineMillis - System.currentTimeMillis()

      if (remainingMillis > 0) {
        runCatching { childProcess.waitFor(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS) }
      }
    }

    childProcesses.forEach { childProcess ->
      if (childProcess.isAlive) {
        runCatching { childProcess.destroyForcibly() }
      }
    }
  }
}

/** A lightweight representation of a spawned process outcome for early wiring. */
data class SysProcessOutcome(
    val exitCode: Int,
    val standardOutput: String,
    val errorOutput: String,
)
