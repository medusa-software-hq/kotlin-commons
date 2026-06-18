package software.medusa.commons.git.tree.realized

import software.medusa.commons.git.tree.GitTreeFile
import software.medusa.commons.git.tree.GitTreeFile.Companion.realizeFile
import software.medusa.commons.git.tree.GitTreeGroup
import software.medusa.commons.git.tree.GitTreeGroup.Companion.realizeGroup
import software.medusa.commons.git.tree.GitTreeNode
import software.medusa.commons.git.tree.GitTreeSubmoduleLink
import software.medusa.commons.git.tree.GitTreeSymlink
import software.medusa.commons.unix.filesystem.UfsReadonlyEntity

internal data object GitRealizedTree_utils {
  fun GitTreeNode.realizeNode(): UfsReadonlyEntity =
      when (this) {
        is GitTreeGroup -> realizeGroup()
        is GitTreeFile -> realizeFile()
        is GitTreeSymlink ->
            throw UnsupportedOperationException("Symlink filesystem views are not supported yet")
        is GitTreeSubmoduleLink ->
            throw UnsupportedOperationException("Submodule filesystem views are not supported yet")
      }
}
