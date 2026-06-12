package software.medusa.commons.git.tree.db

import java.io.InputStream
import java.io.OutputStream
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.ObjectReader
import software.medusa.commons.git.GitFileMode
import software.medusa.commons.git.tree.GitTreeFile

internal class GitDbTreeFile(
    private val jObjectReader: ObjectReader,
    private val jObjectId: ObjectId,
    override val mode: GitFileMode,
) : GitTreeFile() {
  override fun read(): InputStream = jObjectReader.open(jObjectId).openStream()

  override fun write(
      outputStream: OutputStream,
  ) {
    jObjectReader.open(jObjectId).copyTo(outputStream)
  }
}
