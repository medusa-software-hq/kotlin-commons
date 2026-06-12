package software.medusa.commons.git.tree

import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.ObjectInserter
import software.medusa.commons.unix.path.UfsAbsolutePath
import software.medusa.commons.unix.path.UfsPath
import software.medusa.commons.unix.path.UfsRelativePath

data class GitTreeSymlink(
    val targetPath: UfsPath<*>,
) : GitTreeLeaf {
  companion object {
    internal fun GitTreeSymlink.storeSymlink(
        objectInserter: ObjectInserter,
    ): ObjectId {
      val targetPathText =
          when (targetPath) {
            is UfsRelativePath -> targetPath.toUnixRelativePathString()
            is UfsAbsolutePath -> targetPath.toUnixAbsolutePathString()
          }

      return objectInserter.insert(
          Constants.OBJ_BLOB,
          targetPathText.toByteArray(Charsets.UTF_8),
      )
    }
  }
}
