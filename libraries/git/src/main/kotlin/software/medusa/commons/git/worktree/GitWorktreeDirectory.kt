package software.medusa.commons.git.worktree

import java.io.ByteArrayInputStream
import software.medusa.commons.unix.filesystem.UfsReadonlyDirectory
import software.medusa.commons.unix.filesystem.UfsReadonlyDirectoryIndex
import software.medusa.commons.unix.filesystem.UfsReadonlyEntity
import software.medusa.commons.unix.filesystem.UfsReadonlyFile
import software.medusa.commons.unix.path.UfsName

sealed interface GitWorktreeDirectory : GitWorktreeEntity {
  override val asFilesystemEntity: UfsReadonlyDirectory

  override val asFilteredFilesystemEntity: UfsReadonlyDirectory?

  val effectiveFilter: GitWorktreeFilter?

  suspend fun readIndex(): GitWorktreeDirectoryIndex

  suspend fun readChild(name: UfsName.Literal): GitWorktreeEntity?
}

interface GitIncludedWorktreeDirectory : GitWorktreeDirectory {
  fun interface LocalFilterLoader {
    suspend fun loadLocalFilter(
        directory: UfsReadonlyDirectory,
    ): GitWorktreeFilter?
  }

  data object GitignoreLocalFilterLoader : LocalFilterLoader {
    private val gitignoreFileName = UfsName.Literal(".gitignore")

    override suspend fun loadLocalFilter(
        directory: UfsReadonlyDirectory,
    ): GitWorktreeFilter? {
      val gitignoreFile = directory.extract(name = gitignoreFileName) ?: return null

      require(gitignoreFile is UfsReadonlyFile) {
        "Expected $gitignoreFileName to be a file, got ${gitignoreFile::class.simpleName}"
      }

      return GitWorktreeFilter.parse(
          gitignoreInputStream = ByteArrayInputStream(gitignoreFile.read().toByteArray()),
      )
    }
  }

  companion object {
    suspend fun include(
        directory: UfsReadonlyDirectory,
        baseFilter: GitWorktreeFilter,
        localFilterLoader: LocalFilterLoader = GitignoreLocalFilterLoader,
    ): GitIncludedWorktreeDirectory {
      val localFilter = localFilterLoader.loadLocalFilter(directory = directory)
      val effectiveFilter = localFilter?.chain(baseFilter) ?: baseFilter

      return FsGitIncludedWorktreeDirectory(
          localFilterLoader = localFilterLoader,
          directory = directory,
          effectiveFilter = effectiveFilter,
      )
    }
  }

  override val status: GitWorktreeEntity.Status.Considered

  override val asFilteredFilesystemEntity: UfsReadonlyDirectory

  override val effectiveFilter: GitWorktreeFilter
}

class FsGitIncludedWorktreeDirectory(
    private val localFilterLoader: GitIncludedWorktreeDirectory.LocalFilterLoader,
    private val directory: UfsReadonlyDirectory,
    override val effectiveFilter: GitWorktreeFilter,
) : GitIncludedWorktreeDirectory {
  override val status: GitWorktreeEntity.Status.Considered
    get() = GitWorktreeEntity.Status.Considered(GitWorktreeFilter.Classification.Include)

  override val asFilesystemEntity: UfsReadonlyDirectory
    get() = directory

  override val asFilteredFilesystemEntity: UfsReadonlyDirectory
    get() =
        object : UfsReadonlyDirectory {
          override suspend fun readIndex() =
              UfsReadonlyDirectoryIndex(
                  this@FsGitIncludedWorktreeDirectory.readIndex()
                      .childEntityByName
                      .mapNotNull { (name, childEntity) ->
                        val filteredEntity =
                            childEntity.asFilteredFilesystemEntity ?: return@mapNotNull null

                        name to filteredEntity
                      }
                      .toMap(),
              )

          override suspend fun extract(
              name: UfsName.Literal,
          ): UfsReadonlyEntity? = readChild(name)?.asFilteredFilesystemEntity
        }

  override suspend fun readIndex(): GitWorktreeDirectoryIndex =
      GitWorktreeDirectoryIndex(
          directory
              .readIndex()
              .childEntityByName
              .map { (name, entity) ->
                name to
                    GitWorktreeEntity.consider(
                        effectiveFilter = effectiveFilter,
                        name = name,
                        entity = entity,
                        localFilterLoader = localFilterLoader,
                    )
              }
              .toMap(),
      )

  override suspend fun readChild(name: UfsName.Literal): GitWorktreeEntity? {
    val entity = directory.extract(name) ?: return null

    return GitWorktreeEntity.consider(
        effectiveFilter = effectiveFilter,
        name = name,
        entity = entity,
        localFilterLoader = localFilterLoader,
    )
  }
}

sealed interface GitExcludedWorktreeDirectory : GitWorktreeDirectory {
  override val asFilteredFilesystemEntity: Nothing?

  override val effectiveFilter: Nothing?
}

sealed class FsGitExcludedWorktreeDirectory(
    private val directory: UfsReadonlyDirectory,
) : GitExcludedWorktreeDirectory {
  override val asFilesystemEntity: UfsReadonlyDirectory
    get() = directory

  final override val asFilteredFilesystemEntity: Nothing?
    get() = null

  final override val effectiveFilter: Nothing?
    get() = null

  final override suspend fun readIndex(): GitWorktreeDirectoryIndex =
      GitWorktreeDirectoryIndex(
          directory
              .readIndex()
              .childEntityByName
              .map { (name, entity) -> name to GitWorktreeEntity.wrapNonConsidered(entity) }
              .toMap(),
      )

  final override suspend fun readChild(name: UfsName.Literal): GitWorktreeEntity? =
      directory.extract(name)?.let(GitWorktreeEntity::wrapNonConsidered)
}

class GitIgnoredWorktreeDirectory(
    directory: UfsReadonlyDirectory,
) : FsGitExcludedWorktreeDirectory(directory = directory) {
  override val status: GitWorktreeEntity.Status.Considered
    get() = GitWorktreeEntity.Status.Considered(GitWorktreeFilter.Classification.Ignore)
}

class GitNonConsideredWorktreeDirectory(
    directory: UfsReadonlyDirectory,
) : FsGitExcludedWorktreeDirectory(directory = directory) {
  override val status: GitWorktreeEntity.Status.NonConsidered
    get() = GitWorktreeEntity.Status.NonConsidered
}
