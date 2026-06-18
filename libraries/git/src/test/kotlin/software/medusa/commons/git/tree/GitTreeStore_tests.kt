package software.medusa.commons.git.tree

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.FileMode
import org.eclipse.jgit.lib.ObjectChecker
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.treewalk.TreeWalk
import software.medusa.commons.git.GitFileMode
import software.medusa.commons.git.tree.GitTreeGroup.Companion.storeGroup
import software.medusa.commons.git.tree.immutable.GitImmutableTreeFile
import software.medusa.commons.git.tree.immutable.GitImmutableTreeGroup
import software.medusa.commons.unix.path.UfsName

typealias TestTreeGroup = GitImmutableTreeGroup

typealias TestTreeFile = GitImmutableTreeFile

class GitTreeStore_tests {
  @Test
  fun storedTreeCanBeReadBackFromJgit() {
    val helloFileName = "hello.txt"
    val helloFileContent = "hello"
    val nestedDirName = "nested"
    val worldFileName = "world.txt"
    val worldFileContent = "world!"
    val binFileName = "file.bin"
    val binFileContent = ByteString(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())
    val shFileName = "hi.sh"
    val shFileContent = "#!/bin/bash\necho hi"

    val testTreeGroup =
        GitImmutableTreeGroup(
            childNodeByName =
                mapOf(
                    UfsName.Literal(helloFileName) to
                        GitImmutableTreeFile(content = helloFileContent),
                    UfsName.Literal(nestedDirName) to
                        GitImmutableTreeGroup(
                            childNodeByName =
                                mapOf(
                                    UfsName.Literal(worldFileName) to
                                        TestTreeFile(content = worldFileContent),
                                ),
                        ),
                    UfsName.Literal(binFileName) to TestTreeFile(content = binFileContent),
                    UfsName.Literal(shFileName) to
                        TestTreeFile(content = shFileContent, mode = GitFileMode.Executable),
                ),
        )

    val repoPath = Files.createTempDirectory("git-tree-dump-")

    Git.init().setDirectory(repoPath.toFile()).call().repository.use { repository ->
      val treeEntries =
          repository.newObjectInserter().use { objectInserter ->
            val treeId = testTreeGroup.storeGroup(objectInserter)

            ObjectChecker().checkTree(repository.open(treeId).cachedBytes)

            repository.walk(treeId).toSet()
          }

      assertEquals(
          expected =
              setOf(
                  TreeWalkEntry(
                      path = helloFileName,
                      mode = FileMode.REGULAR_FILE,
                      content = helloFileContent.encodeToByteString(),
                  ),
                  TreeWalkEntry(
                      path = "$nestedDirName/$worldFileName",
                      mode = FileMode.REGULAR_FILE,
                      content = worldFileContent.encodeToByteString(),
                  ),
                  TreeWalkEntry(
                      path = binFileName,
                      mode = FileMode.REGULAR_FILE,
                      content = binFileContent,
                  ),
                  TreeWalkEntry(
                      path = shFileName,
                      mode = FileMode.EXECUTABLE_FILE,
                      content = shFileContent.encodeToByteString(),
                  ),
              ),
          actual = treeEntries,
      )
    }
  }
}

private data class TreeWalkEntry(
    val path: String,
    val mode: FileMode,
    val content: ByteString,
)

private fun Repository.walk(treeId: ObjectId): Sequence<TreeWalkEntry> = sequence {
  TreeWalk(this@walk).use { treeWalk ->
    treeWalk.addTree(treeId)
    treeWalk.isRecursive = true

    while (treeWalk.next()) {
      val objectLoader = this@walk.open(treeWalk.getObjectId(0))

      yield(
          TreeWalkEntry(
              path = treeWalk.pathString,
              mode = treeWalk.getFileMode(0),
              content = ByteString(objectLoader.cachedBytes),
          ),
      )
    }
  }
}
