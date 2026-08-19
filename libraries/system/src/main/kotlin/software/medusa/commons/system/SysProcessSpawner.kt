@file:OptIn(InternalCoroutinesApi::class)

package software.medusa.commons.system

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.future.await
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Coordinates spawning native processes for tooling that relies on concrete executables. */
class SysProcessSpawner(
    runtime: Runtime = Runtime.getRuntime(),
) {
  private val childProcesses = ConcurrentHashMap.newKeySet<Process>()

  init {
    runtime.addShutdownHook(Thread { destroyAll() })
  }

  /**
   * Runs [block] against a freshly started process, and ends that process — together with every
   * descendant it left behind — once the block ends, however it ends.
   *
   * [environment] replaces the parent's rather than adding to it, so a child sees what it is given
   * and nothing else. There is no default: what a child may read is worth deciding each time, and a
   * process handed everything its parent holds carries every credential the parent holds with it.
   *
   * @return Whatever [block] returned.
   * @throws IOException If the process could not be started.
   */
  suspend fun <ResultT> executeProcess(
      executable: SysExecutableHandle,
      workingDirectory: Path? = null,
      arguments: List<String>,
      environment: Map<String, String>,
      block: suspend SysProcessScope.() -> ResultT,
  ): ResultT {
    val process =
        withContext(Dispatchers.IO) {
          ProcessBuilder(listOf(executable.path.toString()) + arguments)
              .redirectErrorStream(false)
              .apply {
                workingDirectory?.let { directory(it.toFile()) }
                environment().clear()
                environment().putAll(environment)
              }
              .start()
        }
    childProcesses.add(process)
    process.onExit().thenRun { childProcesses.remove(process) }

    val scope = ProperSysProcessScope(process)
    return coroutineScope {
      try {
        scope.block()
      } finally {
        // The order carries the design. This runs inside `coroutineScope`, so before it joins its
        // children: ending the process closes its pipes, any reader still parked in a blocking read
        // reaches the end, and the join then completes on its own. End it after the join instead
        // and the join is what hangs — for as long as the process cares to stay alive.
        withContext(NonCancellable + Dispatchers.IO) { process.endTree(defaultKillGracePeriod) }
      }
    }
  }

  /**
   * Runs [executable] to completion and collects everything it produced.
   *
   * Both output streams are taken at once, which is what keeps a process writing heavily to both
   * from stalling against a full pipe.
   *
   * @return What the process wrote and the code it ended with.
   * @throws IOException If the process could not be started.
   */
  suspend fun spawn(
      executable: SysExecutableHandle,
      workingDirectory: Path? = null,
      arguments: List<String>,
      environment: Map<String, String>,
  ): SysProcessOutcome =
      executeProcess(
          executable = executable,
          workingDirectory = workingDirectory,
          arguments = arguments,
          environment = environment,
      ) {
        coroutineScope {
          val errorOutput = async { standardError.consumeText() }
          val collectedOutput = standardOutput.consumeText()
          SysProcessOutcome(
              exitCode = awaitExit(),
              standardOutput = collectedOutput,
              errorOutput = errorOutput.await(),
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

  private companion object {
    // Long enough for a tool that flushes something useful when asked to stop, short enough that
    // one which ignores the request does not hold anyone up.
    val defaultKillGracePeriod: Duration = 2.seconds
  }
}

/** What a process wrote, and the code it ended with. */
data class SysProcessOutcome(
    val exitCode: Int,
    val standardOutput: String,
    val errorOutput: String,
)

private class ProperSysProcessScope(private val process: Process) : SysProcessScope {
  private val properStandardOutput = ProperSysOutStream(process.inputStream, "standard output")
  private val properStandardError = ProperSysOutStream(process.errorStream, "standard error")

  override val standardInput: SysInStream = ProperSysInStream(process.outputStream)

  override val standardOutput: SysOutStream
    get() = properStandardOutput

  override val standardError: SysOutStream
    get() = properStandardError

  override suspend fun awaitExit(): Int = coroutineScope {
    // Whatever nobody wanted is read and dropped alongside the wait, so the process is never left
    // stuck against a pipe no one is emptying. Taking it first is what makes this safe against a
    // block reading the same stream: whoever takes it owns it, and only one can.
    if (properStandardOutput.tryTake()) launch { properStandardOutput.discardToEnd() }
    if (properStandardError.tryTake()) launch { properStandardError.discardToEnd() }

    // onExit rather than waitFor: this one answers to cancellation.
    process.onExit().await().exitValue()
    // Leaving here joins the two discards, which end because the exit closed the pipes.
  }
}

private class ProperSysOutStream(
    private val source: InputStream,
    private val name: String,
) : SysOutStream {
  private val taken = AtomicBoolean(false)
  private val reader: BufferedReader = source.bufferedReader()

  /** Takes the stream for the first caller to ask — a consumer, or the discard behind awaitExit. */
  fun tryTake(): Boolean = taken.compareAndSet(false, true)

  override fun consumeLines(): Flow<String> {
    take()
    return flow { readCancellably { reader -> while (true) emit(reader.readLine() ?: break) } }
        // Overrides the buffer flowOn would otherwise fuse in, which would have the reader running
        // whole lines ahead of the collector. What the kernel holds is then the only buffer, and
        // anyone wanting more asks for it downstream.
        .buffer(Channel.RENDEZVOUS)
        .flowOn(Dispatchers.IO)
  }

  override suspend fun consumeText(): String {
    take()
    return withContext(Dispatchers.IO) { readCancellably { it.readText() } }
  }

  suspend fun discardToEnd() {
    withContext(Dispatchers.IO) {
      readCancellably { reader ->
        val buffer = CharArray(discardBufferSize)
        while (reader.read(buffer) != -1) Unit
      }
    }
  }

  private fun take() =
      check(tryTake()) {
        "The process's $name is already being read. A pipe can be read once, so consumeLines() " +
            "and consumeText() are alternative views of the same bytes — take one, once."
      }

  /**
   * Reads with [body], closing the stream if this coroutine is cancelled so that a read parked in a
   * blocking call ends rather than holding its thread until the process does. The failure that
   * closing provokes is turned back into cancellation, so a reader never mistakes it for the stream
   * genuinely breaking.
   */
  private suspend fun <T> readCancellably(body: suspend (BufferedReader) -> T): T {
    val onCancel =
        currentCoroutineContext().job.invokeOnCompletion(onCancelling = true) {
          runCatching { source.close() }
        }
    return try {
      body(reader)
    } catch (e: IOException) {
      currentCoroutineContext().ensureActive()
      throw e
    } finally {
      onCancel.dispose()
    }
  }

  private companion object {
    const val discardBufferSize = 8192
  }
}

private class ProperSysInStream(sink: OutputStream) : SysInStream {
  private val writer = sink.bufferedWriter()
  private val closed = AtomicBoolean(false)

  override suspend fun writeLine(text: String) {
    withContext(Dispatchers.IO) {
      writer.write(text)
      writer.newLine()
      writer.flush()
    }
  }

  override fun close() {
    if (closed.compareAndSet(false, true)) runCatching { writer.close() }
  }
}

/** Ends the process and everything it started, asking first and insisting after [gracePeriod]. */
private fun Process.endTree(gracePeriod: Duration) {
  val descendants = descendants().toList()
  destroy()
  descendants.forEach { runCatching { it.destroy() } }
  if (!waitFor(gracePeriod.inWholeMilliseconds, TimeUnit.MILLISECONDS)) {
    runCatching { destroyForcibly() }
    descendants.forEach { runCatching { it.destroyForcibly() } }
  }
}
