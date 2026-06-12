package software.medusa.commons.git.tree

import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.ObjectInserter
import org.eclipse.jgit.lib.ObjectReader
import software.medusa.commons.git.tree.GitTreeGroup.Companion.realizeGroup
import software.medusa.commons.git.tree.GitTreeGroup.Companion.storeGroup
import software.medusa.commons.git.tree.db.GitDbTreeGroup
import software.medusa.commons.git.tree.interpreted.GitInterpretedTreeGroup.Companion.interpretAsGitTreeGroup
import software.medusa.commons.unix.filesystem.UfsReadonlyDirectory

@JvmInline
value class GitProperTree(
    val rootGroup: GitTreeGroup,
) : GitTree {
  companion object {
    fun load(
        jObjectReader: ObjectReader,
        jTreeId: ObjectId,
    ): GitProperTree =
        GitProperTree(
            rootGroup = GitDbTreeGroup(jObjectReader = jObjectReader, jTreeId = jTreeId),
        )

    internal suspend fun interpretDirectory(
        filesystem: UfsReadonlyDirectory,
    ): GitProperTree? {
      val rootGroup = filesystem.interpretAsGitTreeGroup() ?: return null

      return GitProperTree(rootGroup = rootGroup)
    }
  }

  override fun store(
      jObjectInserter: ObjectInserter,
  ): ObjectId = rootGroup.storeGroup(jObjectInserter = jObjectInserter)

  override fun realize(): UfsReadonlyDirectory = rootGroup.realizeGroup()
}
