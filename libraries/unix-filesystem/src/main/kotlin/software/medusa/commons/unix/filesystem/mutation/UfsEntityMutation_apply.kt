package software.medusa.commons.unix.filesystem.mutation

import software.medusa.commons.unix.filesystem.UfsMutableDirectory
import software.medusa.commons.unix.filesystem.UfsMutableEntity
import software.medusa.commons.unix.filesystem.UfsMutableFile
import software.medusa.commons.unix.filesystem.UfsReadonlyDirectory
import software.medusa.commons.unix.filesystem.UfsReadonlyEntity
import software.medusa.commons.unix.filesystem.UfsReadonlyFile
import software.medusa.commons.unix.filesystem.deleteRecursively
import software.medusa.commons.unix.filesystem.materializeIn
import software.medusa.commons.unix.path.UfsName

/**
 * Applies [mutation] to this entity.
 *
 * @throws IllegalStateException if the mutation's kind does not match the entity's kind (e.g. a
 *   file mutation applied to a directory).
 */
suspend fun UfsMutableEntity.applyMutation(
    mutation: UfsEntityMutation,
) {
  when (mutation) {
    is UfsFileMutation -> {
      val file =
          this as? UfsMutableFile
              ?: throw IllegalStateException("Cannot apply a file mutation to a directory.")

      file.applyMutation(mutation)
    }

    is UfsDirectoryMutation -> {
      val directory =
          this as? UfsMutableDirectory
              ?: throw IllegalStateException("Cannot apply a directory mutation to a file.")

      directory.applyMutation(mutation)
    }
  }
}

/** Applies [mutation] to this file. */
suspend fun UfsMutableFile.applyMutation(
    mutation: UfsFileMutation,
) {
  when (mutation) {
    UfsFileMutation.Delete -> delete()

    is UfsFileMutation.Update -> write(mutation.newContent)
  }
}

/** Applies [mutation] to this directory. */
suspend fun UfsMutableDirectory.applyMutation(
    mutation: UfsDirectoryMutation,
) {
  when (mutation) {
    is UfsDirectoryMutation.Delete ->
        when (mutation.mode) {
          UfsDirectoryMutation.Delete.Mode.NonRecursive -> delete()

          UfsDirectoryMutation.Delete.Mode.Recursive -> deleteRecursively()
        }

    is UfsDirectoryMutation.Dive -> applyDive(mutation)
  }
}

private suspend fun UfsMutableDirectory.applyDive(
    dive: UfsDirectoryMutation.Dive,
) {
  for ((name, operation) in dive.operationByName) {
    when (operation) {
      is UfsDirectoryMutation.Dive.Operation.Create ->
          createFrom(name = name, templateEntity = operation.templateEntity)

      is UfsDirectoryMutation.Dive.Operation.Mutate -> {
        val child =
            extract(name)
                ?: throw IllegalStateException(
                    "Cannot mutate child `${name.content}`, because no such child exists.",
                )

        child.applyMutation(operation.mutation)
      }
    }
  }
}

/**
 * Creates a new child named [name] by materializing the read-only [templateEntity] into this
 * directory.
 */
private suspend fun UfsMutableDirectory.createFrom(
    name: UfsName.Literal,
    templateEntity: UfsReadonlyEntity,
) {
  when (templateEntity) {
    is UfsReadonlyFile -> {
      val createdFile = createFile(name = name, initialContent = templateEntity.read())

      if (templateEntity.isExecutable()) {
        createdFile.makeExecutable()
      }
    }

    is UfsReadonlyDirectory -> {
      val createdDirectory = createDirectory(name)

      templateEntity.materializeIn(createdDirectory)
    }
  }
}
