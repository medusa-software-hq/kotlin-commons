package software.medusa.commons.git.worktree

import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import software.medusa.commons.unix.path.UfsName
import software.medusa.commons.unix.path.UfsRelativePath

private fun GitWorktreeFilter.Companion.parse(
    gitignoreText: String,
): GitWorktreeFilter = parse(ByteArrayInputStream(gitignoreText.toByteArray()))

class GitWorktreeFilter_tests {
  @Test
  fun parsesSimpleIgnoreRules() {
    val filter = GitWorktreeFilter.parse("ignored.txt\n")

    assertEquals(
        GitWorktreeFilter.Classification.Ignore,
        filter.classify(
            path = UfsRelativePath.of(UfsName.Literal("ignored.txt")),
            nodeKind = GitFsNodeKind.File,
        ),
    )

    assertEquals(
        null,
        filter.classify(
            path = UfsRelativePath.of(UfsName.Literal("kept.txt")),
            nodeKind = GitFsNodeKind.File,
        ),
    )
  }

  @Test
  fun chainedFiltersPreferInnerGitignore() {
    val baseFilter = GitWorktreeFilter.parse("*.log\n")
    val localFilter = GitWorktreeFilter.parse("!keep.log\n")
    val chainedFilter = localFilter.chain(baseFilter = baseFilter)

    assertEquals(
        GitWorktreeFilter.Classification.Include,
        chainedFilter.classify(
            path = UfsRelativePath.of(UfsName.Literal("keep.log")),
            nodeKind = GitFsNodeKind.File,
        ),
    )

    assertEquals(
        GitWorktreeFilter.Classification.Ignore,
        chainedFilter.classify(
            path = UfsRelativePath.of(UfsName.Literal("drop.log")),
            nodeKind = GitFsNodeKind.File,
        ),
    )
  }

  @Test
  fun nestedFiltersPrependDirectoryPath() {
    val filter = GitWorktreeFilter.parse("sub/ignored.txt\n")
    val nestedFilter = filter.nest("sub")

    assertEquals(
        GitWorktreeFilter.Classification.Ignore,
        nestedFilter.classify(
            path = UfsRelativePath.of(UfsName.Literal("ignored.txt")),
            nodeKind = GitFsNodeKind.File,
        ),
    )
  }
}
