package software.medusa.commons.git.worktree

import software.medusa.commons.unix.path.UfsName

@JvmInline
value class GitWorktreeDirectoryIndex(
    val childEntityByName: Map<UfsName.Literal, GitWorktreeEntity>,
)
