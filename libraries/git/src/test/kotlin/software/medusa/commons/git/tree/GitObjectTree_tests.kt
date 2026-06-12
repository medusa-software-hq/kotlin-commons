package software.medusa.commons.git.tree

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.CommitBuilder
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.FileMode
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.ObjectInserter
import org.eclipse.jgit.lib.RefUpdate
import org.eclipse.jgit.lib.TreeFormatter
import software.medusa.commons.git.GitCommitDetails
import software.medusa.commons.git.GitCommitHash
import software.medusa.commons.git.GitFileMode
import software.medusa.commons.git.GitPersonalDetails
import software.medusa.commons.git.GitRepository
import software.medusa.commons.git.tree.db.GitDbTreeFile
import software.medusa.commons.git.tree.db.GitDbTreeGroup
import software.medusa.commons.unix.path.UfsAbsolutePath
import software.medusa.commons.unix.path.UfsName

class GitObjectTree_tests {
  @Test
  fun readsDumpedJgitTreeBackIntoGitObjectTreeAbstraction() = runTest {
    val repoPath = Files.createTempDirectory("git-object-tree-")

    Git.init().setDirectory(repoPath.toFile()).call().repository.use { repository ->
      val dumpedTreeId =
          repository.newObjectInserter().use { objectInserter ->
            val helloBlobId = objectInserter.insertBlob("hello")
            val toolBlobId = objectInserter.insertBlob("echo hi")
            val worldBlobId = objectInserter.insertBlob("world")

            val nestedTreeId =
                TreeFormatter()
                    .apply { append("world.txt", FileMode.REGULAR_FILE, worldBlobId) }
                    .insertTo(objectInserter)

            TreeFormatter()
                .apply {
                  append("hello.txt", FileMode.REGULAR_FILE, helloBlobId)
                  append("tool.sh", FileMode.EXECUTABLE_FILE, toolBlobId)
                  append("nested", FileMode.TREE, nestedTreeId)
                }
                .insertTo(objectInserter)
          }

      val commitHash =
          repository.createCommitWithTree(treeId = dumpedTreeId, message = "seed object tree")
      val objectTree =
          GitRepository.open(repoPath).process { readCommit(commitHash).tree.rootGroup }
      val objectTreeIndex = objectTree.readIndex()

      val helloFile =
          assertIs<GitDbTreeFile>(
              objectTreeIndex.childNodeByName.getValue(UfsName.Literal("hello.txt"))
          )
      assertEquals(GitFileMode.Regular, helloFile.mode)
      assertEquals("hello", helloFile.read().bufferedReader().readText())

      val toolFile =
          assertIs<GitDbTreeFile>(
              objectTreeIndex.childNodeByName.getValue(UfsName.Literal("tool.sh"))
          )
      assertEquals(GitFileMode.Executable, toolFile.mode)
      assertEquals("echo hi", toolFile.read().bufferedReader().readText())

      val nestedGroup =
          assertIs<GitDbTreeGroup>(
              objectTreeIndex.childNodeByName.getValue(UfsName.Literal("nested"))
          )
      val nestedFile =
          assertIs<GitDbTreeFile>(
              nestedGroup.readIndex().childNodeByName.getValue(UfsName.Literal("world.txt"))
          )
      assertEquals("world", nestedFile.read().bufferedReader().readText())
    }
  }

  @Test
  fun readsAbsoluteSymlinkObjects() = runTest {
    val repoPath = Files.createTempDirectory("git-object-tree-symlink-")

    Git.init().setDirectory(repoPath.toFile()).call().repository.use { repository ->
      repository.newObjectInserter().use { objectInserter ->
        val symlinkObjectId = objectInserter.insert(Constants.OBJ_BLOB, "/tmp/target".toByteArray())

        val treeId =
            TreeFormatter()
                .apply { append("symlink", FileMode.SYMLINK, symlinkObjectId) }
                .insertTo(objectInserter)

        val commitHash =
            repository.createCommitWithTree(treeId = treeId, message = "seed symlink tree")

        val symlinkNode =
            GitRepository.open(repoPath).process {
              readCommit(commitHash)
                  .tree
                  .rootGroup
                  .readIndex()
                  .childNodeByName
                  .getValue(UfsName.Literal("symlink"))
            }

        val absoluteSymlink = assertIs<GitTreeSymlink>(symlinkNode)

        assertEquals(
            expected = UfsAbsolutePath.of(UfsName.Literal("tmp"), UfsName.Literal("target")),
            actual = absoluteSymlink.targetPath,
        )
      }
    }
  }
}

private fun ObjectInserter.insertBlob(content: String): ObjectId =
    insert(Constants.OBJ_BLOB, content.toByteArray())

private fun org.eclipse.jgit.lib.Repository.createCommitWithTree(
    treeId: ObjectId,
    message: String,
): GitCommitHash {
  val details =
      GitCommitDetails(
          authorDetails = GitPersonalDetails(name = "Test Author", email = "author@example.com"),
          committerDetails =
              GitPersonalDetails(name = "Test Committer", email = "committer@example.com"),
          message = message,
      )

  val commitId =
      newObjectInserter().use { objectInserter ->
        objectInserter.insert(
            CommitBuilder().apply {
              author = details.authorDetails.personIdent
              committer = details.committerDetails?.personIdent ?: details.authorDetails.personIdent
              this.message = details.message
              setTreeId(treeId)
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

    else -> error("Failed to create commit for tree $treeId: $updateResult")
  }
}
