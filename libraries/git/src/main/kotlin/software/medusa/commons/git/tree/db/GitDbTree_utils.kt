package software.medusa.commons.git.tree.db

import org.eclipse.jgit.lib.FileMode
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.ObjectReader
import software.medusa.commons.git.GitCommitHash
import software.medusa.commons.git.GitFileMode
import software.medusa.commons.git.tree.GitTreeNode
import software.medusa.commons.git.tree.GitTreeSubmoduleLink
import software.medusa.commons.git.tree.GitTreeSymlink
import software.medusa.commons.unix.path.UfsAbsolutePath
import software.medusa.commons.unix.path.UfsPath
import software.medusa.commons.unix.path.UfsRelativePath

internal object GitDbTree_utils {
  fun readNode(
      jObjectReader: ObjectReader,
      jChildObjectId: ObjectId,
      jFileMode: FileMode,
  ): GitTreeNode =
      when (jFileMode) {
        FileMode.REGULAR_FILE,
        FileMode.EXECUTABLE_FILE ->
            GitDbTreeFile(
                jObjectReader = jObjectReader,
                jObjectId = jChildObjectId,
                mode =
                    when (jFileMode) {
                      FileMode.REGULAR_FILE -> GitFileMode.Regular
                      FileMode.EXECUTABLE_FILE -> GitFileMode.Executable
                      else -> error("Unexpected file mode: $jFileMode")
                    },
            )

        FileMode.TREE ->
            GitDbTreeGroup(
                jObjectReader = jObjectReader,
                jTreeId = jChildObjectId,
            )

        FileMode.SYMLINK -> {
          val targetPathText =
              jObjectReader.open(jChildObjectId).cachedBytes.toString(Charsets.UTF_8)

          GitTreeSymlink(
              targetPath =
                  when {
                    targetPathText.startsWith(UfsPath.Separator) ->
                        UfsAbsolutePath.parse(targetPathText)

                    else -> UfsRelativePath.parse(targetPathText)
                  },
          )
        }

        FileMode.GITLINK -> GitTreeSubmoduleLink(commitHash = GitCommitHash(jChildObjectId.name))

        else -> error("Unsupported file mode: $jFileMode")
      }
}
