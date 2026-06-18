package software.medusa.commons.unix.filesystem

import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.decodeToString

/**
 * A read-only view of a file in the compatibility filesystem API.
 *
 * Like the rest of the read-only surface, this may represent either a real filesystem entry behind
 * a facade or a file exposed from a generated filesystem-shaped view.
 */
interface UfsReadonlyFile : UfsReadonlyEntity {
  /** Reads the full file contents. */
  suspend fun read(): ByteString

  /** Returns whether the file should be treated as executable. */
  suspend fun isExecutable(): Boolean = false
}

/** Reads the full file contents and decodes them as UTF-8 text. */
suspend fun UfsReadonlyFile.readText(): String = read().decodeToString()
