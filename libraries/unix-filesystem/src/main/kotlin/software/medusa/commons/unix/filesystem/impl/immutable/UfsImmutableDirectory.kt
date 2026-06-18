package software.medusa.commons.unix.filesystem.impl.immutable

import software.medusa.commons.unix.filesystem.UfsReadonlyDirectory
import software.medusa.commons.unix.filesystem.UfsReadonlyDirectoryIndex
import software.medusa.commons.unix.filesystem.UfsReadonlyEntity
import software.medusa.commons.unix.path.UfsName

class UfsImmutableDirectory(
    val childEntityByName: Map<UfsName.Literal, UfsReadonlyEntity>,
) : UfsReadonlyDirectory {
  override suspend fun readIndex(): UfsReadonlyDirectoryIndex =
      UfsReadonlyDirectoryIndex(childEntityByName = childEntityByName)

  override suspend fun extract(name: UfsName.Literal): UfsReadonlyEntity? = childEntityByName[name]
}
