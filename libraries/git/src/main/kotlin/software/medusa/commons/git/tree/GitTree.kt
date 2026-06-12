package software.medusa.commons.git.tree

import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.ObjectInserter
import software.medusa.commons.unix.filesystem.UfsReadonlyDirectory

sealed interface GitTree {
  companion object {
    suspend fun interpretDirectoryAsGitTree(
        directory: UfsReadonlyDirectory,
    ): GitTree = GitProperTree.interpretDirectory(directory) ?: GitEmptyTree

    internal fun GitTreeNode.storeNode(
        objectInserter: ObjectInserter,
    ): ObjectId =
        when (this) {
          is GitTreeGroup ->
              with(GitTreeGroup.Companion) {
                this@storeNode.storeGroup(jObjectInserter = objectInserter)
              }
          is GitTreeFile ->
              with(GitTreeFile.Companion) {
                this@storeNode.storeFile(objectInserter = objectInserter)
              }
          is GitTreeSymlink ->
              with(GitTreeSymlink.Companion) {
                this@storeNode.storeSymlink(objectInserter = objectInserter)
              }
          is GitTreeSubmoduleLink -> ObjectId.fromString(commitHash.raw)
        }
  }

  fun store(
      jObjectInserter: ObjectInserter,
  ): ObjectId

  fun realize(): UfsReadonlyDirectory
}
