package software.medusa.commons.git.tree.interpreted

import java.io.ByteArrayInputStream
import java.io.InputStream
import software.medusa.commons.git.GitFileMode
import software.medusa.commons.git.tree.GitTreeFile
import software.medusa.commons.unix.filesystem.UfsReadonlyFile

internal class GitInterpretedTreeFile
private constructor(
    private val content: ByteArray,
    override val mode: GitFileMode,
) : GitTreeFile() {
  companion object {
    internal suspend fun UfsReadonlyFile.interpretAsGitTreeFile(): GitInterpretedTreeFile =
        GitInterpretedTreeFile(
            content = read().toByteArray(),
            mode = if (isExecutable()) GitFileMode.Executable else GitFileMode.Regular,
        )
  }

  override fun read(): InputStream = ByteArrayInputStream(content)
}
