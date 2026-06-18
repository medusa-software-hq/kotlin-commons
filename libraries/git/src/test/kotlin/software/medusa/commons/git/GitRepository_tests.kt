package software.medusa.commons.git

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.CommitBuilder
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.RefUpdate
import org.eclipse.jgit.lib.Repository
import software.medusa.commons.git.tree.GitProperTree
import software.medusa.commons.git.tree.GitTreeFile
import software.medusa.commons.git.tree.TestGitTreeFile
import software.medusa.commons.git.tree.TestGitTreeGroup
import software.medusa.commons.unix.path.UfsName

class GitRepository_tests {
  @Test
  fun openResolvesCommitRefsAndCreatesNewRefs() = runTest {
    val repoPath = Files.createTempDirectory("git-repository-")

    Git.init().setDirectory(repoPath.toFile()).call().use { git ->
      val initialCommitHash =
          git.repository.createInitialCommit(
              details =
                  GitCommitDetails(
                      authorDetails =
                          GitPersonalDetails(name = "Test Author", email = "author@example.com"),
                      committerDetails =
                          GitPersonalDetails(
                              name = "Test Committer",
                              email = "committer@example.com",
                          ),
                      message = "initial",
                  ),
          )

      val repository = GitRepository.open(repoPath)
      val headRef = GitRefPath.of("HEAD")
      val featureRef = GitRefPath.of("refs", "heads", "feature")

      repository.process {
        assertEquals(initialCommitHash, resolveCommitRef(headRef))
        assertNull(resolveCommitRef(GitRefPath.of("refs", "heads", "missing")))

        val createdRef =
            createCommitRef(
                commitHash = initialCommitHash,
                newRefPath = featureRef,
            )

        assertEquals(GitRef(featureRef), createdRef)
        assertEquals(initialCommitHash, resolveCommitRef(featureRef))
      }
    }
  }

  @Test
  fun createCommitAndReadCommitRoundTrip() = runTest {
    val repoPath = Files.createTempDirectory("git-repository-commit-")

    Git.init().setDirectory(repoPath.toFile()).call().use { git ->
      val initialCommitHash =
          git.repository.createInitialCommit(
              details =
                  GitCommitDetails(
                      authorDetails =
                          GitPersonalDetails(name = "Base Author", email = "base@example.com"),
                      committerDetails =
                          GitPersonalDetails(
                              name = "Base Committer",
                              email = "base-committer@example.com",
                          ),
                      message = "initial",
                  ),
          )

      val repository = GitRepository.open(repoPath)

      val details =
          GitCommitDetails(
              authorDetails =
                  GitPersonalDetails(name = "Test Author", email = "author@example.com"),
              committerDetails =
                  GitPersonalDetails(name = "Test Committer", email = "committer@example.com"),
              message = "add file",
          )

      val tree =
          GitProperTree(
              rootGroup =
                  TestGitTreeGroup(
                      childNodeByName =
                          mapOf(UfsName.Literal("hello.txt") to TestGitTreeFile(content = "hello")),
                  ),
          )

      repository.process {
        val commitHash =
            createCommit(
                parentCommitHash = initialCommitHash,
                details = details,
                tree = tree,
            )

        val commit = readCommit(commitHash)
        val helloFile =
            assertNotNull(
                commit.tree.rootGroup.readIndex().childNodeByName[UfsName.Literal("hello.txt")]
            )

        assertEquals(setOf(initialCommitHash), commit.parentCommitHashes)
        assertEquals(details, commit.details)
        assertEquals("hello", (helloFile as GitTreeFile).read().bufferedReader().readText())
      }
    }
  }
}

private fun Repository.createInitialCommit(
    details: GitCommitDetails,
): GitCommitHash {
  val commitId =
      newObjectInserter().use { objectInserter ->
        val emptyTreeId = objectInserter.insert(Constants.OBJ_TREE, ByteArray(0))

        objectInserter.insert(
            CommitBuilder().apply {
              author = details.authorDetails.personIdent
              committer = details.committerDetails?.personIdent ?: details.authorDetails.personIdent
              message = details.message
              setTreeId(emptyTreeId)
            },
        )
      }

  val headTargetRefName = exactRef(Constants.HEAD).target.name
  val refUpdate = updateRef(headTargetRefName)
  refUpdate.setNewObjectId(commitId)

  return when (val updateResult = refUpdate.update()) {
    RefUpdate.Result.NEW,
    RefUpdate.Result.FAST_FORWARD,
    RefUpdate.Result.NO_CHANGE,
    -> GitCommitHash(commitId.name)

    else -> error("Failed to create initial commit ref $headTargetRefName: $updateResult")
  }
}
