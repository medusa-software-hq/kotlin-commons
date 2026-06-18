package software.medusa.commons.git.tree.interpreted

import software.medusa.commons.git.tree.GitTreeNode
import software.medusa.commons.git.tree.interpreted.GitInterpretedTreeFile.Companion.interpretAsGitTreeFile
import software.medusa.commons.git.tree.interpreted.GitInterpretedTreeGroup.Companion.interpretAsGitTreeGroup
import software.medusa.commons.unix.filesystem.UfsReadonlyDirectory
import software.medusa.commons.unix.filesystem.UfsReadonlyEntity
import software.medusa.commons.unix.filesystem.UfsReadonlyFile

internal data object GitInterpretedTree_utils {
  suspend fun interpretAsGitTreeNode(
      entity: UfsReadonlyEntity,
  ): GitTreeNode? =
      when (entity) {
        is UfsReadonlyDirectory -> entity.interpretAsGitTreeGroup()
        is UfsReadonlyFile -> entity.interpretAsGitTreeFile()
      }
}
