package software.medusa.commons.system

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
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
}
