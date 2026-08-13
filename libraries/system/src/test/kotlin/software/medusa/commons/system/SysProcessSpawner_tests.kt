package software.medusa.commons.system

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest

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
  fun `launch streams stdout line by line`() = runTest {
    val spawner = SysProcessSpawner()
    val handle =
        spawner.launch(
            executable = SysExecutableHandle.resolve(executablePath = Path.of("/bin/sh")),
            arguments = listOf("-c", "echo one; echo two; echo three"),
        )

    val lines = handle.standardOutputLines.toList()
    val termination = handle.awaitTermination()
    handle.close()

    assertEquals(listOf("one", "two", "three"), lines)
    assertEquals(0, termination.exitCode)
  }

  @Test
  fun `launch feeds stdin and streams the response`() = runTest {
    val spawner = SysProcessSpawner()
    val handle =
        spawner.launch(
            executable = SysExecutableHandle.resolve(executablePath = Path.of("/bin/sh")),
            arguments = listOf("-c", "read line; echo \"got:\$line\""),
        )

    handle.writeLine("ping")
    val lines = handle.standardOutputLines.toList()
    val termination = handle.awaitTermination()
    handle.close()

    assertEquals(listOf("got:ping"), lines)
    assertEquals(0, termination.exitCode)
  }

  @Test
  fun `launch captures stderr separately from stdout`() = runTest {
    val spawner = SysProcessSpawner()
    val handle =
        spawner.launch(
            executable = SysExecutableHandle.resolve(executablePath = Path.of("/bin/sh")),
            arguments = listOf("-c", "echo oops >&2"),
        )

    val lines = handle.standardOutputLines.toList()
    val termination = handle.awaitTermination()
    handle.close()

    assertEquals(emptyList(), lines)
    assertEquals("oops\n", termination.errorOutput)
  }

  @Test
  fun `close terminates a still-running process`() = runTest {
    val spawner = SysProcessSpawner()
    val handle =
        spawner.launch(
            executable = SysExecutableHandle.resolve(executablePath = Path.of("/bin/sh")),
            arguments = listOf("-c", "sleep 30"),
        )

    handle.close()
    val termination = handle.awaitTermination()

    // A forcibly-killed process exits non-zero rather than hanging for 30s.
    assertTrue(termination.exitCode != 0)
  }
}
