package software.medusa.commons.system

import kotlinx.coroutines.flow.Flow

/**
 * A live handle to a running process, returned by [SysProcessSpawner.launch].
 *
 * The batch [SysProcessSpawner.spawn] reads all output and then returns, which cannot drive a
 * long-running process that emits progress incrementally or must be fed input while it runs. This
 * handle exposes those capabilities instead: consume [standardOutputLines] as they arrive,
 * optionally [writeLine] to stdin, then [awaitTermination] for the exit code and captured stderr.
 * Always [close] — it kills the process tree — so a `use { }` block is the intended pattern.
 */
interface SysProcessHandle : AutoCloseable {
  /**
   * The process's stdout, one line at a time, in order; the flow completes when stdout closes.
   * Cold: collecting it consumes the single underlying stream, so collect it once.
   */
  val standardOutputLines: Flow<String>

  /** Writes [text] followed by a newline to the process's stdin and flushes. */
  suspend fun writeLine(text: String)

  /**
   * Closes the process's stdin, signalling end-of-input, without terminating the process. A child
   * that reads stdin to EOF (many CLIs do, even when the real work comes from arguments) blocks
   * until this is called. Idempotent.
   */
  fun closeInput()

  /**
   * Suspends until the process exits, then reports the exit code and the stderr captured so far.
   */
  suspend fun awaitTermination(): SysProcessTermination

  /** Kills the process and all of its descendants. Idempotent; safe after normal termination. */
  override fun close()
}

/** How a launched process ended. */
data class SysProcessTermination(
    val exitCode: Int,
    val errorOutput: String,
)
