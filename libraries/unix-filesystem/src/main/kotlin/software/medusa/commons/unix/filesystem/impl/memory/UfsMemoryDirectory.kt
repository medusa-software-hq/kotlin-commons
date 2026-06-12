package software.medusa.commons.unix.filesystem.impl.memory

import kotlinx.io.bytestring.ByteString
import software.medusa.commons.unix.filesystem.UfsMutableDirectory
import software.medusa.commons.unix.filesystem.UfsMutableDirectoryIndex
import software.medusa.commons.unix.filesystem.UfsMutableEntity
import software.medusa.commons.unix.filesystem.UfsMutableFile
import software.medusa.commons.unix.path.UfsName

class UfsMemoryDirectory(
    private val onDelete: (() -> Unit) = {},
) : UfsMutableDirectory {
  private val entityByName = mutableMapOf<UfsName.Literal, UfsMutableEntity>()

  private var wasDeleted = false

  override suspend fun readIndex(): UfsMutableDirectoryIndex =
      UfsMutableDirectoryIndex(childEntityByName = entityByName.toMap())

  override suspend fun extract(
      name: UfsName.Literal,
  ): UfsMutableEntity? = entityByName[name]

  override suspend fun createFile(
      name: UfsName.Literal,
      initialContent: ByteString?,
  ): UfsMutableFile {
    if (entityByName.contains(name)) {
      throw IllegalStateException(
          "Cannot create file with name ${name.content}, because an entity with the same name already exists.",
      )
    }

    val newFile =
        UfsMemoryFile(
            initialPath = initialContent ?: UfsMemoryFile.emptyByteString,
            onDelete = { entityByName.remove(name) },
        )

    entityByName[name] = newFile

    return newFile
  }

  override suspend fun createDirectory(
      name: UfsName.Literal,
  ): UfsMutableDirectory {
    if (entityByName.contains(name)) {
      throw IllegalStateException(
          "Cannot create directory with name ${name.content}, because an entity with the same name already exists.",
      )
    }

    val newDirectory =
        UfsMemoryDirectory(
            onDelete = { entityByName.remove(name) },
        )

    entityByName[name] = newDirectory

    return newDirectory
  }

  override suspend fun delete() {
    if (wasDeleted) {
      throw IllegalStateException("Directory was already deleted.")
    }

    if (entityByName.isNotEmpty()) {
      throw IllegalStateException(
          "Cannot delete directory, because it is not empty.",
      )
    }

    onDelete()

    wasDeleted = true
  }
}
