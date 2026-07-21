package software.medusa.commons.unix.filesystem

import software.medusa.commons.unix.path.UfsLiteralRelativePath
import software.medusa.commons.unix.path.UfsName

/**
 * A read-only view of a directory in the compatibility filesystem API.
 *
 * This abstraction is intended both for directories backed by a real filesystem and for generated
 * directory views assembled in memory or derived from some other source.
 */
interface UfsReadonlyDirectory : UfsReadonlyEntity {
  suspend fun readIndex(): UfsReadonlyDirectoryIndex

  /** Returns the direct child named [name], or `null` when no such child exists. */
  suspend fun extract(
      name: UfsName.Literal,
  ): UfsReadonlyEntity?

  companion object {
    val Empty: UfsReadonlyDirectory =
        object : UfsReadonlyDirectory {
          override suspend fun readIndex(): UfsReadonlyDirectoryIndex =
              UfsReadonlyDirectoryIndex(emptyMap())

          override suspend fun extract(name: UfsName.Literal): UfsReadonlyEntity? = null
        }
  }
}

/**
 * Traverses [relativePath] starting from this entity.
 *
 * Returns `null` when any path component does not exist or when traversal would need to descend
 * through a file.
 */
suspend fun UfsReadonlyEntity.extractDeepReadonly(
    relativePath: UfsLiteralRelativePath,
): UfsReadonlyEntity? {
  var currentEntity: UfsReadonlyEntity = this

  for (name in relativePath.names) {
    if (currentEntity !is UfsReadonlyDirectory) {
      return null
    }

    val nextEntity = currentEntity.extract(name) ?: return null

    currentEntity = nextEntity
  }

  return currentEntity
}

/**
 * Copies all direct and nested entries from this directory into [targetDirectory].
 *
 * Existing entries at matching names are overwritten. Entries present only in [targetDirectory] are
 * left untouched.
 */
suspend fun UfsReadonlyDirectory.copyRecursivelyTo(
    targetDirectory: UfsMutableDirectory,
) {
  copyRecursivelyToImpl(targetDirectory)
}

private suspend fun UfsReadonlyDirectory.copyRecursivelyToImpl(
    targetDirectory: UfsMutableDirectory,
) {
  for ((name, sourceEntity) in readIndex().childEntityByName) {
    val existingTargetEntity = targetDirectory.extract(name)

    when (sourceEntity) {
      is UfsReadonlyFile -> {
        val sourceContent = sourceEntity.read()

        val targetFile =
            when (existingTargetEntity) {
              null ->
                  targetDirectory.createFile(
                      name = name,
                      initialContent = sourceContent,
                  )

              is UfsMutableFile -> existingTargetEntity.also { it.write(sourceContent) }

              is UfsMutableDirectory -> {
                existingTargetEntity.deleteRecursively()

                targetDirectory.createFile(
                    name = name,
                    initialContent = sourceContent,
                )
              }
            }

        // Mirror the source file's executable bit. Without this, `copyRecursivelyTo` dropped it
        // (unlike its sibling `materializeIn`), silently stripping 100755 from files such as
        // `gradlew` — which then surfaced as spurious mode-only diffs downstream.
        if (sourceEntity.isExecutable()) {
          targetFile.makeExecutable()
        }
      }

      is UfsReadonlyDirectory -> {
        val targetSubdirectory =
            when (existingTargetEntity) {
              null -> targetDirectory.createDirectory(name)

              is UfsMutableDirectory -> existingTargetEntity

              is UfsMutableFile -> {
                existingTargetEntity.delete()

                targetDirectory.createDirectory(name)
              }
            }

        sourceEntity.copyRecursivelyToImpl(targetSubdirectory)
      }
    }
  }
}

/**
 * Recursively copies all direct and nested entries from this directory into [targetDirectory],
 * assumed to be initially empty.
 */
suspend fun UfsReadonlyDirectory.materializeIn(
    targetDirectory: UfsMutableDirectory,
) {
  readIndex().childEntityByName.forEach { (name, entity) ->
    materializeChildIn(
        name = name,
        sourceEntity = entity,
        targetDirectory = targetDirectory,
    )
  }
}

private suspend fun materializeChildIn(
    name: UfsName.Literal,
    sourceEntity: UfsReadonlyEntity,
    targetDirectory: UfsMutableDirectory,
) {
  when (sourceEntity) {
    is UfsReadonlyFile -> {
      val targetFile =
          targetDirectory.createFile(
              name = name,
              initialContent = sourceEntity.read(),
          )

      if (sourceEntity.isExecutable()) {
        targetFile.makeExecutable()
      }
    }

    is UfsReadonlyDirectory -> {
      val targetSubdirectory = targetDirectory.createDirectory(name)

      sourceEntity.readIndex().childEntityByName.forEach { (childName, childEntity) ->
        materializeChildIn(
            name = childName,
            sourceEntity = childEntity,
            targetDirectory = targetSubdirectory,
        )
      }
    }
  }
}
