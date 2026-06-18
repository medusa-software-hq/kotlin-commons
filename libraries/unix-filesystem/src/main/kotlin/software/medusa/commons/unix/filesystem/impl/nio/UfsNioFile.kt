package software.medusa.commons.unix.filesystem.impl.nio

import java.nio.file.Path
import kotlin.io.path.deleteExisting
import kotlin.io.path.isExecutable
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.bytestring.ByteString
import software.medusa.commons.unix.filesystem.UfsMutableFile

class UfsNioFile(
    private val filePath: Path,
) : UfsMutableFile {
  override suspend fun read(): ByteString =
      withContext(Dispatchers.IO) { ByteString(filePath.readBytes()) }

  override suspend fun write(newContent: ByteString) =
      withContext(Dispatchers.IO) { filePath.writeBytes(newContent.toByteArray()) }

  override suspend fun makeExecutable() {
    withContext(Dispatchers.IO) { filePath.toFile().setExecutable(true) }
  }

  override suspend fun isExecutable(): Boolean =
      withContext(Dispatchers.IO) { filePath.isExecutable() }

  override suspend fun delete() = withContext(Dispatchers.IO) { filePath.deleteExisting() }
}
