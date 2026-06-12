package software.medusa.commons.git.tree

import org.eclipse.jgit.lib.FileMode
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.ObjectInserter
import org.eclipse.jgit.lib.TreeFormatter
import software.medusa.commons.git.tree.GitTree.Companion.storeNode
import software.medusa.commons.git.tree.realized.GitRealizedDirectory
import software.medusa.commons.unix.filesystem.UfsReadonlyDirectory
import software.medusa.commons.unix.path.UfsName

abstract class GitTreeGroup : GitTreeNode {
  data object Empty : GitTreeGroup() {
    override suspend fun readIndex(): Index = Index.Empty
  }

  @JvmInline
  value class Index(
      val childNodeByName: Map<UfsName.Literal, GitTreeNode>,
  ) {
    companion object {
      val Empty = Index(childNodeByName = emptyMap())
    }
  }

  companion object {
    internal fun GitTreeGroup.storeGroup(
        jObjectInserter: ObjectInserter,
    ): ObjectId {
      val treeFormatter = TreeFormatter()
      val index = kotlinx.coroutines.runBlocking { readIndex() }

      index.childNodeByName.entries
          .sortedBy { (name, child) ->
            if (child is GitTreeGroup) "${name.content}/" else name.content
          }
          .forEach { (name, child) ->
            val childObjectId = child.storeNode(jObjectInserter)

            val fileMode =
                when (child) {
                  is GitTreeGroup -> FileMode.TREE
                  is GitTreeFile -> child.mode.jFileMode
                  is GitTreeSymlink -> FileMode.SYMLINK
                  is GitTreeSubmoduleLink -> FileMode.GITLINK
                }

            treeFormatter.append(name.content, fileMode, childObjectId)
          }

      return treeFormatter.insertTo(jObjectInserter)
    }

    internal fun GitTreeGroup.realizeGroup(): UfsReadonlyDirectory =
        GitRealizedDirectory(treeGroup = this)
  }

  abstract suspend fun readIndex(): Index
}
