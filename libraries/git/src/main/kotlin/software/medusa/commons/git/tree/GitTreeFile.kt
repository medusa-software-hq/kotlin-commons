package software.medusa.commons.git.tree

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.ObjectInserter
import software.medusa.commons.git.GitFileMode
import software.medusa.commons.git.tree.realized.GitRealizedFile
import software.medusa.commons.unix.filesystem.UfsReadonlyFile

abstract class GitTreeFile : GitTreeLeaf {
  companion object {
    internal fun GitTreeFile.storeFile(
        objectInserter: ObjectInserter,
    ): ObjectId =
        ByteArrayOutputStream().use { outputStream ->
          write(outputStream)
          objectInserter.insert(Constants.OBJ_BLOB, outputStream.toByteArray())
        }

    internal fun GitTreeFile.realizeFile(): UfsReadonlyFile = GitRealizedFile(treeFile = this)
  }

  abstract val mode: GitFileMode

  abstract fun read(): InputStream

  open fun write(
      outputStream: OutputStream,
  ) {
    read().use { it.copyTo(outputStream) }
  }
}
