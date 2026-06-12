package software.medusa.commons.git.tree.interpreted

import software.medusa.commons.git.tree.GitTreeGroup
import software.medusa.commons.unix.filesystem.UfsReadonlyDirectory
import software.medusa.commons.unix.filesystem.isEmpty
import software.medusa.commons.unix.filesystem.isNotEmpty
import software.medusa.commons.unix.filesystem.split

/**
 * @param directory A non-empty directory that can be interpreted as a Git tree. The contents of
 *   this directory is assumed to be stable, i.e. it won't change during the lifetime of this
 *   object.
 */
internal class GitInterpretedTreeGroup
private constructor(
    private val directory: UfsReadonlyDirectory,
) : GitTreeGroup() {
  companion object {
    internal suspend fun UfsReadonlyDirectory.interpretAsGitTreeGroup(): GitInterpretedTreeGroup? {
      val baseIndex = readIndex()

      if (baseIndex.isEmpty()) {
        // We allow empty trees only at the top level
        return null
      }

      val splitIndex = baseIndex.split()

      return when {
        // If there's at least on file in the directory, we know it forms a valid Git subtree
        splitIndex.fileIndex.isNotEmpty() ->
            GitInterpretedTreeGroup(
                directory = this,
            )

        else -> { // If there are only subdirectories, we need to perform a deeper test
          val childDirectories = splitIndex.directoryIndex.childEntityByName.values

          // If _any_ of the subdirectories can be interpreted as a Git subtree, this directory
          // can be interpreted as a Git subtree as well.
          val isValidTree = childDirectories.any {
            // Note: We discard the result of the interpretation
            it.interpretAsGitTreeGroup() != null
          }

          when {
            isValidTree ->
                GitInterpretedTreeGroup(
                    directory = this,
                )

            else -> null
          }
        }
      }
    }
  }

  override suspend fun readIndex(): Index {
    val baseIndex = directory.readIndex()

    return Index(
        childNodeByName =
            baseIndex.childEntityByName
                .mapNotNull { (name, entity) ->
                  val treeNode =
                      GitInterpretedTree_utils.interpretAsGitTreeNode(
                          entity = entity,
                      ) ?: return@mapNotNull null

                  name to treeNode
                }
                .toMap(),
    )
  }
}
