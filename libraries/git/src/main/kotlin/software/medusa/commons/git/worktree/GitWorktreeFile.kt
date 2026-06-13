package software.medusa.commons.git.worktree

import software.medusa.commons.unix.filesystem.UfsReadonlyFile

class GitWorktreeFile(
    private val file: UfsReadonlyFile,
    override val status: GitWorktreeEntity.Status,
) : GitWorktreeEntity {
  override val asFilesystemEntity: UfsReadonlyFile
    get() = file

  override val asFilteredFilesystemEntity: UfsReadonlyFile?
    get() =
        when (status) {
          is GitWorktreeEntity.Status.Considered ->
              file.takeIf { status.classification == GitWorktreeFilter.Classification.Include }

          GitWorktreeEntity.Status.NonConsidered -> null
        }
}
