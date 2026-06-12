package software.medusa.commons.git.tree.realized

import software.medusa.commons.git.tree.GitTreeGroup
import software.medusa.commons.git.tree.realized.GitRealizedTree_utils.realizeNode
import software.medusa.commons.unix.filesystem.UfsReadonlyDirectory
import software.medusa.commons.unix.filesystem.UfsReadonlyDirectoryIndex
import software.medusa.commons.unix.filesystem.UfsReadonlyEntity
import software.medusa.commons.unix.path.UfsName

class GitRealizedDirectory(
    private val treeGroup: GitTreeGroup,
) : UfsReadonlyDirectory {
  override suspend fun readIndex(): UfsReadonlyDirectoryIndex =
      UfsReadonlyDirectoryIndex(
          treeGroup.readIndex().childNodeByName.mapValues { (_, childNode) ->
            childNode.realizeNode()
          },
      )

  override suspend fun extract(
      name: UfsName.Literal,
  ): UfsReadonlyEntity? = treeGroup.readIndex().childNodeByName[name]?.realizeNode()
}
