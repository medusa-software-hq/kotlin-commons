package software.medusa.commons.system

import java.io.BufferedWriter
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
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

  /**
   * Launches [executable] and returns a live [SysProcessHandle] for streaming interaction. Unlike
   * [spawn], output is not collected up front: the caller consumes stdout as it arrives and may
   * feed stdin while the process runs. The process is tracked for shutdown cleanup exactly like
   * [spawn]'s.
   */
  fun launch(
      executable: SysExecutableHandle,
      workingDirectory: Path? = null,
      arguments: List<String> = emptyList(),
      environment: Map<String, String> = System.getenv(),
  ): SysProcessHandle {
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

    return LaunchedProcess(process)
  }

  private class LaunchedProcess(
      private val process: Process,
  ) : SysProcessHandle {
    private val stdin: BufferedWriter = process.outputStream.bufferedWriter()

    // stderr is drained on a daemon thread so the child cannot block on a full pipe mid-run.
    private val errorBuffer = StringBuilder()
    private val errorThread =
        Thread {
              process.errorStream.bufferedReader().useLines { lines ->
                lines.forEach { line -> synchronized(errorBuffer) { errorBuffer.appendLine(line) } }
              }
            }
            .apply {
              isDaemon = true
              start()
            }

    override val standardOutputLines: Flow<String> =
        flow {
              process.inputStream.bufferedReader().useLines { lines ->
                for (line in lines) {
                  emit(line)
                }
              }
            }
            .flowOn(Dispatchers.IO)

    override suspend fun writeLine(text: String) {
      withContext(Dispatchers.IO) {
        stdin.write(text)
        stdin.newLine()
        stdin.flush()
      }
    }

    override suspend fun awaitTermination(): SysProcessTermination {
      val exitCode = withContext(Dispatchers.IO) { process.waitFor() }
      errorThread.join(errorJoinMillis)
      return SysProcessTermination(
          exitCode = exitCode,
          errorOutput = synchronized(errorBuffer) { errorBuffer.toString() },
      )
    }

    override fun close() {
      // Kill descendants first, then the root, so nothing reparents and survives.
      process.descendants().forEach { runCatching { it.destroyForcibly() } }
      runCatching { process.destroyForcibly() }
      runCatching { process.waitFor(closeWaitSeconds, TimeUnit.SECONDS) }
      runCatching { stdin.close() }
    }

    private companion object {
      const val errorJoinMillis = 2_000L
      const val closeWaitSeconds = 5L
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
