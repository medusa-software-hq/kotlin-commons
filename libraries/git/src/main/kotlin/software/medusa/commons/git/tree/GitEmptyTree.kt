package software.medusa.commons.git.tree

import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.ObjectInserter
import software.medusa.commons.unix.filesystem.UfsReadonlyDirectory

data object GitEmptyTree : GitTree {
  override fun store(
      jObjectInserter: ObjectInserter,
  ): ObjectId = jObjectInserter.insert(Constants.OBJ_TREE, ByteArray(0))

  override fun realize(): UfsReadonlyDirectory = UfsReadonlyDirectory.Empty
}
