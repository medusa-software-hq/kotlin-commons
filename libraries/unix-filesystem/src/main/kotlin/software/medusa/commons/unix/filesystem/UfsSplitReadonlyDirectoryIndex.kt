package software.medusa.commons.unix.filesystem

import software.medusa.commons.unix.path.UfsName

/** A split of the direct children of a directory into files and directories. */
data class UfsSplitReadonlyDirectoryIndex(
    val directoryIndex: UfsDirectoryIndex<UfsReadonlyDirectory>,
    val fileIndex: UfsDirectoryIndex<UfsReadonlyFile>,
)

/** Lists the direct children of this directory, splitting them into files and directories. */
suspend fun UfsReadonlyDirectory.readIndexSplit(): UfsSplitReadonlyDirectoryIndex {
  val directories = mutableMapOf<UfsName.Literal, UfsReadonlyDirectory>()
  val files = mutableMapOf<UfsName.Literal, UfsReadonlyFile>()

  for ((name, entity) in readIndex().childEntityByName) {
    when (entity) {
      is UfsReadonlyDirectory -> directories[name] = entity
      is UfsReadonlyFile -> files[name] = entity
    }
  }

  return UfsSplitReadonlyDirectoryIndex(
      directoryIndex = UfsDirectoryIndex(directories),
      fileIndex = UfsDirectoryIndex(files),
  )
}

/** Checks if this directory index is empty. */
fun UfsReadonlyDirectoryIndex.isEmpty(): Boolean = childEntityByName.isEmpty()

/** Checks if this directory index is not empty. */
fun UfsReadonlyDirectoryIndex.isNotEmpty(): Boolean = childEntityByName.isNotEmpty()

/** Splits this directory index into files and directories. */
suspend fun UfsReadonlyDirectoryIndex.split(): UfsSplitReadonlyDirectoryIndex {
  val directories = mutableMapOf<UfsName.Literal, UfsReadonlyDirectory>()
  val files = mutableMapOf<UfsName.Literal, UfsReadonlyFile>()

  for ((name, entity) in childEntityByName) {
    when (entity) {
      is UfsReadonlyDirectory -> directories[name] = entity
      is UfsReadonlyFile -> files[name] = entity
    }
  }

  return UfsSplitReadonlyDirectoryIndex(
      directoryIndex = UfsDirectoryIndex(directories),
      fileIndex = UfsDirectoryIndex(files),
  )
}
