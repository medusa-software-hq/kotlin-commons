package software.medusa.commons.git.tree.realized

import kotlinx.io.bytestring.ByteString
import software.medusa.commons.git.GitFileMode
import software.medusa.commons.git.tree.GitTreeFile
import software.medusa.commons.unix.filesystem.UfsReadonlyFile

class GitRealizedFile(
    private val treeFile: GitTreeFile,
) : UfsReadonlyFile {
  override suspend fun read(): ByteString = ByteString(treeFile.read().readBytes())

  override suspend fun isExecutable(): Boolean = treeFile.mode == GitFileMode.Executable
}
