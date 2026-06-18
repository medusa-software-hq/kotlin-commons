package software.medusa.commons.git.utils.jgit

import org.eclipse.jgit.lib.FileMode
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.ObjectReader
import org.eclipse.jgit.treewalk.TreeWalk

data class TreeWalkEntry(
    val name: String,
    val objectId: ObjectId,
    val mode: FileMode,
) {
  companion object {
    /**
     * Walk the tree with ID [treeId] (non-recursively) using the provided [objectReader]. This is
     * blocking I/O.
     *
     * [block] is invoked with a sequence of [TreeWalkEntry]s representing the entries in the tree.
     * The sequence has to be consumed within the [block] and cannot be used after the [block]
     * returns.
     */
    fun <R> walk(
        objectReader: ObjectReader,
        treeId: ObjectId,
        shouldRecurse: Boolean,
        block: (Sequence<TreeWalkEntry>) -> R,
    ): R =
        TreeWalk(objectReader).use { treeWalk ->
          treeWalk.addTree(treeId)
          treeWalk.isRecursive = shouldRecurse

          val entrySequence = sequence {
            while (treeWalk.next()) {
              val name = treeWalk.nameString
              val objectId = treeWalk.getObjectId(0)
              val fileMode = treeWalk.getFileMode(0)

              yield(
                  TreeWalkEntry(
                      name = name,
                      objectId = objectId,
                      mode = fileMode,
                  ),
              )
            }
          }

          block(entrySequence)
        }
  }
}
