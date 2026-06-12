package software.medusa.commons.git.tree.immutable

import software.medusa.commons.git.tree.GitTreeGroup
import software.medusa.commons.git.tree.GitTreeNode
import software.medusa.commons.unix.path.UfsName

class GitImmutableTreeGroup(
    childNodeByName: Map<UfsName.Literal, GitTreeNode>,
) : GitTreeGroup() {
  private val index = Index(childNodeByName = childNodeByName)

  override suspend fun readIndex(): Index = index
}
