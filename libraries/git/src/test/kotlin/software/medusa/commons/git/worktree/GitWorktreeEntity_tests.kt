package software.medusa.commons.git.worktree

import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import software.medusa.commons.unix.filesystem.UfsReadonlyDirectory
import software.medusa.commons.unix.path.UfsName

private fun GitWorktreeFilter.Companion.parse(
    gitignoreText: String,
): GitWorktreeFilter = parse(ByteArrayInputStream(gitignoreText.toByteArray()))

class GitWorktreeEntity_tests {
  @Test
  fun considerateDirectoryAppliesBaseFilterAndLocalGitignoreToFilteredView() = runTest {
    val worktree =
        GitIncludedWorktreeDirectory.include(
            directory =
                TestGitWorktreeDirectory(
                    childByName =
                        mapOf(
                            ".gitignore" to
                                TestGitWorktreeFile(content = "!keep.log\nlocal-ignore.txt\n"),
                            "keep.log" to TestGitWorktreeFile(content = "keep"),
                            "drop.log" to TestGitWorktreeFile(content = "drop"),
                            "local-ignore.txt" to TestGitWorktreeFile(content = "ignored"),
                            "keep.txt" to TestGitWorktreeFile(content = "keep-txt"),
                            "sub" to
                                TestGitWorktreeDirectory(
                                    childByName =
                                        mapOf(
                                            ".gitignore" to
                                                TestGitWorktreeFile(
                                                    content = "nested-ignore.txt\n"
                                                ),
                                            "nested-ignore.txt" to
                                                TestGitWorktreeFile(content = "ignored"),
                                            "nested-keep.txt" to
                                                TestGitWorktreeFile(content = "nested-keep"),
                                        ),
                                ),
                        ),
                ),
            baseFilter = GitWorktreeFilter.parse("*.log\n"),
        )

    assertEquals(
        GitWorktreeEntity.Status.Considered(GitWorktreeFilter.Classification.Include),
        worktree.status,
    )

    val filteredRoot = worktree.asFilteredFilesystemEntity

    assertNotNull(filteredRoot.extract(UfsName.Literal("keep.log")))
    assertNull(filteredRoot.extract(UfsName.Literal("drop.log")))
    assertNull(filteredRoot.extract(UfsName.Literal("local-ignore.txt")))

    assertEquals(
        setOf(".gitignore", "keep.log", "keep.txt", "sub"),
        filteredRoot.readIndex().childEntityByName.keys.map { it.content }.toSet(),
    )

    val filteredSubdirectory =
        assertIs<UfsReadonlyDirectory>(filteredRoot.extract(UfsName.Literal("sub")))

    assertNull(filteredSubdirectory.extract(UfsName.Literal("nested-ignore.txt")))
    assertNotNull(filteredSubdirectory.extract(UfsName.Literal("nested-keep.txt")))
    assertEquals(
        setOf(".gitignore", "nested-keep.txt"),
        filteredSubdirectory.readIndex().childEntityByName.keys.map { it.content }.toSet(),
    )
  }

  @Test
  fun ignoredDirectoryCanStillBeTraversedAsNonConsidered() = runTest {
    val root =
        GitIncludedWorktreeDirectory.include(
            directory =
                TestGitWorktreeDirectory(
                    childByName =
                        mapOf(
                            "build" to
                                TestGitWorktreeDirectory(
                                    childByName =
                                        mapOf(
                                            "artifact.txt" to
                                                TestGitWorktreeFile(content = "artifact")
                                        ),
                                ),
                        ),
                ),
            baseFilter = GitWorktreeFilter.parse("build/\n"),
        )

    val buildDirectory =
        assertIs<GitExcludedWorktreeDirectory>(root.readChild(UfsName.Literal("build")))

    assertEquals(
        GitWorktreeEntity.Status.Considered(GitWorktreeFilter.Classification.Ignore),
        buildDirectory.status,
    )
    assertNull(buildDirectory.asFilteredFilesystemEntity)

    val artifactFile =
        assertIs<GitWorktreeFile>(buildDirectory.readChild(UfsName.Literal("artifact.txt")))

    assertEquals(GitWorktreeEntity.Status.NonConsidered, artifactFile.status)
    assertNull(artifactFile.asFilteredFilesystemEntity)
  }

  @Test
  fun filteredViewKeepsDirectoriesEvenWhenAllChildrenAreFilteredOut() = runTest {
    val worktree =
        GitIncludedWorktreeDirectory.include(
            directory =
                TestGitWorktreeDirectory(
                    childByName =
                        mapOf(
                            "empty-dir" to
                                TestGitWorktreeDirectory(
                                    childByName =
                                        mapOf(
                                            "ignored.txt" to
                                                TestGitWorktreeFile(content = "ignored")
                                        ),
                                ),
                        ),
                ),
            baseFilter = GitWorktreeFilter.parse("empty-dir/ignored.txt\n"),
        )

    val filteredRoot = worktree.asFilteredFilesystemEntity
    val filteredDirectory =
        assertIs<UfsReadonlyDirectory>(filteredRoot.extract(UfsName.Literal("empty-dir")))

    assertEquals(
        setOf("empty-dir"),
        filteredRoot.readIndex().childEntityByName.keys.map { it.content }.toSet(),
    )
    assertEquals(emptyMap(), filteredDirectory.readIndex().childEntityByName)
  }
}
