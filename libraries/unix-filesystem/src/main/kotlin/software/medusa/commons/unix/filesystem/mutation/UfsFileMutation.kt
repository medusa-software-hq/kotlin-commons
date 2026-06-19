package software.medusa.commons.unix.filesystem.mutation

import kotlinx.io.bytestring.ByteString

/** A described change to a file. */
sealed class UfsFileMutation : UfsEntityMutation() {
  /** Removes the file. */
  data object Delete : UfsFileMutation()

  /** Replaces the full file contents with [newContent]. */
  data class Update(
      val newContent: ByteString,
  ) : UfsFileMutation()
}
