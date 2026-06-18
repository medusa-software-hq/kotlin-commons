package software.medusa.commons.git.worktree

import software.medusa.commons.unix.filesystem.UfsReadonlyDirectory

@JvmInline
value class GitWorktree(
    val rootDirectory: GitIncludedWorktreeDirectory,
) {
  companion object {
    suspend fun load(
        repoDirectory: UfsReadonlyDirectory,
        globalFilter: GitWorktreeFilter = GitWorktreeFilter.Passive,
    ): GitWorktree =
        GitWorktree(
            GitIncludedWorktreeDirectory.include(
                directory = repoDirectory,
                baseFilter = globalFilter,
            ),
        )
  }
}
