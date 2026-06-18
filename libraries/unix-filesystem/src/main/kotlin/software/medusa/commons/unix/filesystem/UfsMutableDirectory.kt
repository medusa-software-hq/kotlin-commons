package software.medusa.commons.unix.filesystem

import kotlinx.io.bytestring.ByteString
import software.medusa.commons.unix.path.UfsName

/**
 * A mutable directory in the compatibility filesystem API.
 *
 * This is designed to work equally well for adapters over a real writable filesystem and for
 * in-memory directory implementations used in tests, staging, or generated workspaces.
 */
interface UfsMutableDirectory : UfsMutableEntity, UfsReadonlyDirectory {
  override suspend fun readIndex(): UfsMutableDirectoryIndex

  /** Returns the mutable direct child named [name], or `null` when no such child exists. */
  override suspend fun extract(name: UfsName.Literal): UfsMutableEntity?

  /**
   * Creates a new file named [name].
   *
   * Assumes that no file or directory with the same name already exists.
   */
  suspend fun createFile(
      name: UfsName.Literal,
      initialContent: ByteString?,
  ): UfsMutableFile

  /**
   * Creates a new directory named [name].
   *
   * Assumes that no file or directory with the same name already exists.
   */
  suspend fun createDirectory(
      name: UfsName.Literal,
  ): UfsMutableDirectory
}

/**
 * Returns the mutable direct child named [name], creating a new directory if no such child exists.
 *
 * @throws IllegalStateException if a file with the same name already exists.
 */
suspend fun UfsMutableDirectory.extractOrCreateDirectory(
    name: UfsName.Literal,
): UfsMutableDirectory =
    when (val existingEntity = extract(name = name)) {
      is UfsMutableDirectory -> existingEntity

      is UfsMutableFile -> {
        throw IllegalStateException(
            "Expected a directory named `${name.content}`, but found a file"
        )
      }

      null -> createDirectory(name = name)
    }
