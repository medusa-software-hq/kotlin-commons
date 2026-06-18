package software.medusa.commons.unix.filesystem

import kotlinx.io.bytestring.ByteString

/**
 * A mutable file in the compatibility filesystem API.
 *
 * Implementations may write through to a real filesystem or act as an in-memory equivalent for
 * callers that need file-like behavior without depending on a concrete storage backend.
 */
interface UfsMutableFile : UfsMutableEntity, UfsReadonlyFile {
  /** Replaces the full file contents with [newContent]. */
  suspend fun write(newContent: ByteString)

  /** Marks the file as executable. */
  suspend fun makeExecutable()
}
