package software.medusa.commons.git.tree

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import software.medusa.commons.git.GitFileMode
import software.medusa.commons.git.tree.interpreted.GitInterpretedTreeFile
import software.medusa.commons.git.tree.interpreted.GitInterpretedTreeGroup
import software.medusa.commons.git.tree.interpreted.GitInterpretedTreeGroup.Companion.interpretAsGitTreeGroup
import software.medusa.commons.git.worktree.GitIncludedWorktreeDirectory
import software.medusa.commons.git.worktree.GitWorktreeFilter
import software.medusa.commons.git.worktree.TestGitWorktreeDirectory
import software.medusa.commons.git.worktree.TestGitWorktreeFile
import software.medusa.commons.unix.path.UfsName

class GitWorktreeTree_tests {
  @Test
  fun projectsFilesystemViewIntoTreeNodes() = runTest {
    val worktree =
        GitIncludedWorktreeDirectory.include(
                directory =
                    TestGitWorktreeDirectory(
                        childByName =
                            mapOf(
                                "dir" to
                                    TestGitWorktreeDirectory(
                                        childByName =
                                            mapOf(
                                                "nested.txt" to
                                                    TestGitWorktreeFile(content = "hello")
                                            ),
                                    ),
                                "script.sh" to
                                    TestGitWorktreeFile(content = "echo hi", executable = true),
                            ),
                    ),
                baseFilter = GitWorktreeFilter.Passive,
            )
            .asFilteredFilesystemEntity

    val projectedGroup = assertNotNull(worktree.interpretAsGitTreeGroup())
    val projectedGroupIndex = projectedGroup.readIndex()
    val dirGroup =
        assertIs<GitInterpretedTreeGroup>(
            projectedGroupIndex.childNodeByName.getValue(UfsName.Literal("dir"))
        )
    val dirGroupIndex = dirGroup.readIndex()
    val nestedFile =
        assertIs<GitInterpretedTreeFile>(
            dirGroupIndex.childNodeByName.getValue(UfsName.Literal("nested.txt"))
        )

    assertEquals(GitFileMode.Regular, nestedFile.mode)
    assertEquals("hello", nestedFile.read().bufferedReader().readText())

    val scriptFile =
        assertIs<GitInterpretedTreeFile>(
            projectedGroupIndex.childNodeByName.getValue(UfsName.Literal("script.sh"))
        )

    assertEquals(GitFileMode.Executable, scriptFile.mode)
    assertEquals("echo hi", scriptFile.read().bufferedReader().readText())
  }

  @Test
  fun returnsNullForEmptyProjectedDirectory() = runTest {
    val group = TestGitWorktreeDirectory(emptyMap()).interpretAsGitTreeGroup()

    assertNull(group)
  }
}
