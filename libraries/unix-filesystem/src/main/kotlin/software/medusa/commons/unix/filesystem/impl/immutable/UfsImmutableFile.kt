package software.medusa.commons.unix.filesystem.impl.immutable

import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import software.medusa.commons.unix.filesystem.UfsReadonlyFile

class UfsImmutableFile(
    val content: ByteString,
) : UfsReadonlyFile {
  constructor(
      content: String,
  ) : this(content = content.encodeToByteString())

  override suspend fun read(): ByteString = content
}
