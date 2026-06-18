package software.medusa.commons.git.worktree

import kotlinx.io.bytestring.ByteString
import software.medusa.commons.unix.filesystem.UfsReadonlyDirectory
import software.medusa.commons.unix.filesystem.UfsReadonlyDirectoryIndex
import software.medusa.commons.unix.filesystem.UfsReadonlyEntity
import software.medusa.commons.unix.filesystem.UfsReadonlyFile
import software.medusa.commons.unix.path.UfsName

internal class TestGitWorktreeDirectory(
    val childByName: Map<String, UfsReadonlyEntity>,
) : UfsReadonlyDirectory {
  suspend fun read(name: String): UfsReadonlyEntity? = extract(UfsName.Literal(name))

  override suspend fun extract(
      name: UfsName.Literal,
  ): UfsReadonlyEntity? = childByName[name.content]

  override suspend fun readIndex(): UfsReadonlyDirectoryIndex =
      UfsReadonlyDirectoryIndex(
          childByName.mapKeys { (name, _) -> UfsName.Literal(name) },
      )
}

internal class TestGitWorktreeFile(
    private val content: ByteArray,
    private val executable: Boolean = false,
) : UfsReadonlyFile {
  constructor(
      content: String,
      executable: Boolean = false,
  ) : this(
      content = content.toByteArray(Charsets.UTF_8),
      executable = executable,
  )

  override suspend fun read(): ByteString = ByteString(content)

  override suspend fun isExecutable(): Boolean = executable
}
