package software.medusa.commons.git.tree

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import kotlinx.io.bytestring.decodeToString
import software.medusa.commons.git.GitFileMode
import software.medusa.commons.unix.filesystem.UfsReadonlyDirectory
import software.medusa.commons.unix.filesystem.UfsReadonlyFile
import software.medusa.commons.unix.path.UfsName
import software.medusa.commons.unix.path.UfsRelativePath

class GitTree_tests {
  @Test
  fun realizeProjectsTreeNodesIntoFilesystemView() = runTest {
    val tree =
        GitProperTree(
            rootGroup =
                TestGitTreeGroup(
                    childNodeByName =
                        mapOf(
                            UfsName.Literal("nested") to
                                TestGitTreeGroup(
                                    childNodeByName =
                                        mapOf(
                                            UfsName.Literal("hello.txt") to
                                                TestGitTreeFile(content = "hello"),
                                        ),
                                ),
                            UfsName.Literal("tool.sh") to
                                TestGitTreeFile(content = "echo hi", mode = GitFileMode.Executable),
                            UfsName.Literal("tool-link") to
                                GitTreeSymlink(
                                    targetPath = UfsRelativePath.of(UfsName.Literal("tool.sh")),
                                ),
                        ),
                ),
        )

    val rootDirectory = tree.realize()

    val nestedDirectory =
        assertIs<UfsReadonlyDirectory>(rootDirectory.extract(UfsName.Literal("nested")))
    val helloFile = assertIs<UfsReadonlyFile>(nestedDirectory.extract(UfsName.Literal("hello.txt")))
    val toolFile = assertIs<UfsReadonlyFile>(rootDirectory.extract(UfsName.Literal("tool.sh")))

    assertEquals("hello", helloFile.read().decodeToString())
    assertEquals("echo hi", toolFile.read().decodeToString())
    assertEquals(true, toolFile.isExecutable())

    assertFailsWith<UnsupportedOperationException> {
      rootDirectory.extract(UfsName.Literal("tool-link"))
    }
  }
}
