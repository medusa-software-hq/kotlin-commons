package software.medusa.commons.git.worktree

import software.medusa.commons.unix.filesystem.UfsReadonlyDirectory
import software.medusa.commons.unix.filesystem.UfsReadonlyEntity
import software.medusa.commons.unix.filesystem.UfsReadonlyFile

enum class GitFsNodeKind {
  Directory,
  File,
}

val UfsReadonlyEntity.fsNodeKind: GitFsNodeKind
  get() =
      when (this) {
        is UfsReadonlyDirectory -> GitFsNodeKind.Directory
        is UfsReadonlyFile -> GitFsNodeKind.File
      }
