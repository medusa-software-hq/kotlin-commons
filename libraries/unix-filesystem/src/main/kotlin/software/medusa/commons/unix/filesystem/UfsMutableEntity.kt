package software.medusa.commons.unix.filesystem

import software.medusa.commons.unix.path.UfsLiteralRelativePath

/**
 * A mutable filesystem entity exposed through the compatibility API.
 *
 * Implementations are expected to be backed either by a real writable filesystem or by a mutable
 * in-memory stand-in that behaves similarly enough for callers that do not care about the storage
 * medium.
 */
sealed interface UfsMutableEntity : UfsReadonlyEntity {
  /** Deletes this entity. Directories must be empty before deletion. */
  suspend fun delete()
}

/** Deletes this entity and, if it is a directory, all nested children recursively. */
suspend fun UfsMutableEntity.deleteRecursively() {
  when (this) {
    is UfsMutableFile -> delete()

    is UfsMutableDirectory -> {
      for (entity in readIndex().childEntityByName.values) {
        entity.deleteRecursively()
      }

      delete()
    }
  }
}

/**
 * Traverses [relativePath] starting from this entity.
 *
 * Returns `null` when any path component does not exist or when traversal would need to descend
 * through a file.
 */
suspend fun UfsMutableEntity.extractDeepMutable(
    relativePath: UfsLiteralRelativePath,
): UfsMutableEntity? {
  var currentEntity: UfsMutableEntity = this

  for (name in relativePath.names) {
    if (currentEntity !is UfsMutableDirectory) {
      return null
    }

    val nextEntity = currentEntity.extract(name) ?: return null

    currentEntity = nextEntity
  }

  return currentEntity
}
