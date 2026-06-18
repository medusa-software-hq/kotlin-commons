package software.medusa.commons.git.worktree

import software.medusa.commons.git.worktree.GitWorktreeFilter.Classification
import software.medusa.commons.unix.filesystem.UfsReadonlyDirectory
import software.medusa.commons.unix.filesystem.UfsReadonlyEntity
import software.medusa.commons.unix.filesystem.UfsReadonlyFile
import software.medusa.commons.unix.path.UfsLiteralRelativePath
import software.medusa.commons.unix.path.UfsName

sealed interface GitWorktreeEntity {
  sealed interface Status {
    companion object {
      val included: Considered =
          Considered(
              classification = Classification.Include,
          )
    }

    data class Considered(
        val classification: Classification,
    ) : Status

    data object NonConsidered : Status
  }

  companion object {
    suspend fun consider(
        effectiveFilter: GitWorktreeFilter,
        name: UfsName.Literal,
        entity: UfsReadonlyEntity,
        localFilterLoader: GitIncludedWorktreeDirectory.LocalFilterLoader,
    ): GitWorktreeEntity {
      val classification =
          effectiveFilter.classifyEffectively(
              path = UfsLiteralRelativePath.of(name),
              nodeKind = entity.fsNodeKind,
          )

      val status = Status.Considered(classification = classification)

      return when (entity) {
        is UfsReadonlyDirectory ->
            when (classification) {
              Classification.Ignore -> GitIgnoredWorktreeDirectory(directory = entity)

              Classification.Include ->
                  GitIncludedWorktreeDirectory.include(
                      directory = entity,
                      baseFilter = effectiveFilter.nest(name.content),
                      localFilterLoader = localFilterLoader,
                  )
            }

        is UfsReadonlyFile -> GitWorktreeFile(file = entity, status = status)
      }
    }

    fun wrapNonConsidered(entity: UfsReadonlyEntity): GitWorktreeEntity =
        when (entity) {
          is UfsReadonlyDirectory -> GitNonConsideredWorktreeDirectory(directory = entity)
          is UfsReadonlyFile -> GitWorktreeFile(file = entity, status = Status.NonConsidered)
        }
  }

  val status: Status

  val asFilesystemEntity: UfsReadonlyEntity

  val asFilteredFilesystemEntity: UfsReadonlyEntity?
}
