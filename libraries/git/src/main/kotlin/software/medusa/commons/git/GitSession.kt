package software.medusa.commons.git

import java.nio.file.Path
import org.eclipse.jgit.lib.CommitBuilder
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectInserter
import org.eclipse.jgit.lib.ObjectReader
import org.eclipse.jgit.lib.RefUpdate
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import software.medusa.commons.git.tree.GitTree
import software.medusa.commons.git.worktree.GitIncludedWorktreeDirectory
import software.medusa.commons.git.worktree.GitWorktreeFilter
import software.medusa.commons.unix.filesystem.impl.nio.UfsNioDirectory
import software.medusa.commons.unix.filesystem.materializeIn

class GitSession
internal constructor(
    internal val jRepository: Repository,
    internal val jObjectReader: ObjectReader,
    internal val jObjectInserter: ObjectInserter,
) {
  fun resolveHead(): GitCommitHash =
      resolveCommitRef(GitRefPath.of(Constants.HEAD))
          ?: error("Expected HEAD to resolve to a commit")

  fun resolveCommitRef(
      commitRef: GitRefPath,
  ): GitCommitHash? =
      jRepository.resolve(commitRef.toRefString())?.let { resolvedObjectId ->
        GitCommitHash(raw = resolvedObjectId.name)
      }

  fun createCommitRef(
      commitHash: GitCommitHash,
      newRefPath: GitRefPath,
  ): GitRef {
    val refUpdate = jRepository.updateRef(newRefPath.toRefString())
    refUpdate.setNewObjectId(commitHash.objectId)

    return when (val updateResult = refUpdate.update()) {
      RefUpdate.Result.NEW,
      RefUpdate.Result.FAST_FORWARD,
      RefUpdate.Result.FORCED,
      RefUpdate.Result.NO_CHANGE,
      -> GitRef(path = newRefPath)

      else -> error("Failed to update ref ${newRefPath.toRefString()}: $updateResult")
    }
  }

  fun readCommit(
      commitHash: GitCommitHash,
  ): GitCommit =
      RevWalk(jObjectReader).use { revWalk ->
        val revCommit = revWalk.parseCommit(commitHash.objectId)

        revCommit.wrap(session = this)
      }

  fun createCommit(
      parentCommitHash: GitCommitHash,
      details: GitCommitDetails,
      tree: GitTree,
  ): GitCommitHash {
    val jTreeId = tree.store(jObjectInserter = jObjectInserter)

    val jCommitId =
        jObjectInserter.insert(
            CommitBuilder().apply {
              setParentId(parentCommitHash.objectId)
              author = details.authorDetails.personIdent
              committer = details.committerDetails?.personIdent ?: details.authorDetails.personIdent
              message = details.message
              setTreeId(jTreeId)
            },
        )

    jObjectInserter.flush()

    return GitCommitHash(raw = jCommitId.name)
  }

  suspend fun checkOut(
      sourceCommitHash: GitCommitHash,
      targetWorktreePath: Path,
  ) {
    val sourceCommit = readCommit(commitHash = sourceCommitHash)

    sourceCommit.tree
        .realize()
        .materializeIn(
            targetDirectory = UfsNioDirectory(directoryPath = targetWorktreePath),
        )
  }

  suspend fun checkIn(
      parentCommitHash: GitCommitHash,
      sourceWorktreePath: Path,
      commitDetails: GitCommitDetails,
  ): GitCommitHash {
    val filteredWorktree =
        GitIncludedWorktreeDirectory.include(
                directory = UfsNioDirectory(directoryPath = sourceWorktreePath),
                baseFilter = GitWorktreeFilter.Passive,
            )
            .asFilteredFilesystemEntity

    val sourceTree = GitTree.interpretDirectoryAsGitTree(directory = filteredWorktree)

    return createCommit(
        parentCommitHash = parentCommitHash,
        details = commitDetails,
        tree = sourceTree,
    )
  }
}

private fun RevCommit.wrap(session: GitSession): GitCommit =
    GitCommit(
        session = session,
        jRevCommit = this,
    )
