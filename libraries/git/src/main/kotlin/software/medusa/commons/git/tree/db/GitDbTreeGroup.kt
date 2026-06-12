package software.medusa.commons.git.tree.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.ObjectReader
import software.medusa.commons.git.tree.GitTreeGroup
import software.medusa.commons.git.utils.jgit.TreeWalkEntry
import software.medusa.commons.unix.path.UfsName

internal class GitDbTreeGroup(
    private val jObjectReader: ObjectReader,
    private val jTreeId: ObjectId,
) : GitTreeGroup() {
  override suspend fun readIndex(): Index {
    val childNodeByName =
        withContext(Dispatchers.IO) {
          TreeWalkEntry.walk(
              objectReader = jObjectReader,
              treeId = jTreeId,
              shouldRecurse = false,
          ) { entries ->
            entries.associate { entry ->
              UfsName.Literal(entry.name) to
                  GitDbTree_utils.readNode(
                      jObjectReader = jObjectReader,
                      jChildObjectId = entry.objectId,
                      jFileMode = entry.mode,
                  )
            }
          }
        }

    return Index(childNodeByName = childNodeByName)
  }
}
