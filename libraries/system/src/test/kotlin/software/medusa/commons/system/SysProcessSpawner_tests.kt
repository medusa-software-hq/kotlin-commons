package software.medusa.commons.system

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull

class SysProcessSpawner_tests {
  @Test
  fun `SysExecutableHandle stores executable path`() {
    val executable = SysExecutableHandle(path = Path.of("/bin/echo"))

    assertEquals(Path.of("/bin/echo"), executable.path)
  }

  @Test
  fun `resolve accepts valid executable path`() {
    val executable = SysExecutableHandle.resolve(executablePath = Path.of("/bin/echo"))

    assertEquals(Path.of("/bin/echo"), executable.path)
  }

  @Test
  fun `locate finds executable on path`() {
    val executable = SysExecutableHandle.locate(commandName = "echo")

    assertEquals("echo", executable.path.fileName.toString())
  }

  @Test
  fun `spawn captures command output`() = runTest {
    val spawner = SysProcessSpawner()
    val result =
        spawner.spawn(
            executable = SysExecutableHandle.resolve(executablePath = Path.of("/bin/echo")),
            arguments = listOf("hello"),
        )

    assertEquals(0, result.exitCode)
    assertEquals("hello\n", result.standardOutput)
    assertEquals("", result.errorOutput)
  }

  @Test
  fun `executeProcess streams standard output line by line`() = runTest {
    val lines =
        SysProcessSpawner().executeProcess(
            executable = shell(),
            arguments = listOf("-c", "echo one; echo two; echo three"),
        ) {
          standardOutput.consumeLines().toList()
        }

    assertEquals(listOf("one", "two", "three"), lines)
  }

  @Test
  fun `a block wanting only the exit code does not stall a chatty process`() = runTest {
    // Nothing is read here. Unless awaitExit drains what nobody took, a process writing more than
    // a pipe holds would block forever against it.
    val exitCode =
        SysProcessSpawner().executeProcess(
            executable = shell(),
            arguments = listOf("-c", "yes hello | head -c 400000; yes err | head -c 400000 >&2"),
        ) {
          awaitExit()
        }

    assertEquals(0, exitCode)
  }

  @Test
  fun `standard output can be taken once, either way`() = runTest {
    assertFailsWith<IllegalStateException> {
      SysProcessSpawner().executeProcess(executable = shell(), arguments = listOf("-c", "echo x")) {
        standardOutput.consumeLines()
        standardOutput.consumeText()
      }
    }
  }

  @Test
  fun `text is what was written, not lines rejoined`() = runTest {
    val text =
        SysProcessSpawner().executeProcess(
            executable = shell(),
            arguments = listOf("-c", "printf 'a\nb'"),
        ) {
          standardOutput.consumeText()
        }

    // No trailing newline invented, so the caller can tell one was never written.
    assertEquals("a\nb", text)
  }

  @Test
  fun `leaving the block ends a process that would outlive it`() = runTest {
    val elapsed = measureTime {
      SysProcessSpawner().executeProcess(
          executable = shell(),
          arguments = listOf("-c", "sleep 30"),
      ) {
        // Walk away without waiting for anything.
      }
    }

    assertTrue(elapsed < 10.seconds, "leaving the block waited for the process: $elapsed")
  }

  @Test
  fun `abandoning a read does not hold the block open`() = runTest {
    val elapsed = measureTime {
      SysProcessSpawner().executeProcess(
          executable = shell(),
          arguments = listOf("-c", "echo first; sleep 30"),
      ) {
        // The process goes quiet with the stream still open; give up on it rather than wait.
        withTimeoutOrNull(1.seconds) { standardOutput.consumeLines().toList() }
      }
    }

    assertTrue(elapsed < 10.seconds, "a parked read held the block open: $elapsed")
  }

  @Test
  fun `spawn collects both streams and the exit code`() = runTest {
    val outcome =
        SysProcessSpawner()
            .spawn(
                executable = shell(),
                arguments = listOf("-c", "echo out; echo err >&2; exit 3"),
            )

    assertEquals(3, outcome.exitCode)
    assertEquals("out\n", outcome.standardOutput)
    assertEquals("err\n", outcome.errorOutput)
  }

  private fun shell() = SysExecutableHandle.resolve(executablePath = Path.of("/bin/sh"))
}
