package software.medusa.commons.unix.filesystem.impl.nio

import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
import kotlin.io.path.deleteExisting
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.writeBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.bytestring.ByteString
import software.medusa.commons.unix.filesystem.UfsMutableDirectory
import software.medusa.commons.unix.filesystem.UfsMutableDirectoryIndex
import software.medusa.commons.unix.filesystem.UfsMutableEntity
import software.medusa.commons.unix.filesystem.UfsMutableFile
import software.medusa.commons.unix.path.UfsName

class UfsNioDirectory(
    val directoryPath: Path,
) : UfsMutableDirectory {
  companion object {
    val Root = UfsNioDirectory(directoryPath = Path.of("/"))
  }

  override suspend fun readIndex(): UfsMutableDirectoryIndex =
      withContext(Dispatchers.IO) {
        UfsMutableDirectoryIndex(
            directoryPath.listDirectoryEntries().associate { entityPath ->
              val name =
                  UfsName.Literal(
                      content = entityPath.name,
                  )

              val compatEntity =
                  UfsNioEntity_utils.load(
                      entityPath = entityPath,
                  )

              name to compatEntity
            },
        )
      }

  override suspend fun extract(
      name: UfsName.Literal,
  ): UfsMutableEntity? =
      withContext(Dispatchers.IO) {
        val entityPath = directoryPath.resolve(name.content)

        when {
          entityPath.exists() ->
              UfsNioEntity_utils.load(
                  entityPath = entityPath,
              )

          else -> null
        }
      }

  override suspend fun createFile(
      name: UfsName.Literal,
      initialContent: ByteString?,
  ): UfsMutableFile =
      withContext(Dispatchers.IO) {
        val newFilePath = directoryPath.resolve(name.content)

        newFilePath.createFile()

        if (initialContent != null) {
          newFilePath.writeBytes(initialContent.toByteArray())
        }

        UfsNioFile(
            filePath = newFilePath,
        )
      }

  override suspend fun createDirectory(
      name: UfsName.Literal,
  ): UfsMutableDirectory =
      withContext(Dispatchers.IO) {
        val newDirectoryPath = directoryPath.resolve(name.content)

        newDirectoryPath.createDirectory()

        UfsNioDirectory(
            directoryPath = newDirectoryPath,
        )
      }

  override suspend fun delete() = withContext(Dispatchers.IO) { directoryPath.deleteExisting() }
}
