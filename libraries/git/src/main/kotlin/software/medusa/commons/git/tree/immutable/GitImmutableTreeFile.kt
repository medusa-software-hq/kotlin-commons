package software.medusa.commons.git.tree.immutable

import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import software.medusa.commons.git.GitFileMode
import software.medusa.commons.git.tree.GitTreeFile

class GitImmutableTreeFile(
    private val content: ByteString,
    override val mode: GitFileMode = GitFileMode.Regular,
) : GitTreeFile() {
  constructor(
      content: String,
      mode: GitFileMode = GitFileMode.Regular,
  ) : this(
      content = content.encodeToByteString(),
      mode = mode,
  )

  override fun read(): InputStream = ByteArrayInputStream(content.toByteArray())
}
