package software.medusa.commons.git.worktree

import java.io.InputStream
import org.eclipse.jgit.ignore.IgnoreNode
import software.medusa.commons.unix.path.UfsLiteralRelativePath
import software.medusa.commons.unix.path.UfsName
import software.medusa.commons.unix.path.UfsRelativePath

interface GitWorktreeFilter {
  data object Passive : GitWorktreeFilter {
    override fun classify(
        path: UfsLiteralRelativePath,
        nodeKind: GitFsNodeKind,
    ): Classification? = null
  }

  data object GitCheckedOutWorktreeFilter : GitWorktreeFilter {
    val gitDatabaseName = UfsName.Literal(".git")

    override fun classify(
        path: UfsLiteralRelativePath,
        nodeKind: GitFsNodeKind,
    ): Classification? =
        when {
          path == UfsRelativePath.of(gitDatabaseName) -> Classification.Ignore
          else -> null
        }
  }

  companion object {
    fun parse(
        gitignoreInputStream: InputStream,
    ): GitWorktreeFilter {
      val ignoreNode = IgnoreNode()
      ignoreNode.parse(gitignoreInputStream)

      return object : GitWorktreeFilter {
        override fun classify(
            path: UfsLiteralRelativePath,
            nodeKind: GitFsNodeKind,
        ): Classification? =
            when (
                ignoreNode.isIgnored(
                    path.toUnixRelativePathString(),
                    nodeKind == GitFsNodeKind.Directory,
                )
            ) {
              IgnoreNode.MatchResult.CHECK_PARENT -> null
              IgnoreNode.MatchResult.CHECK_PARENT_NEGATE_FIRST_MATCH -> null
              IgnoreNode.MatchResult.IGNORED -> Classification.Ignore
              IgnoreNode.MatchResult.NOT_IGNORED -> Classification.Include
            }
      }
    }
  }

  enum class Classification {
    Ignore,
    Include,
  }

  fun classify(
      path: UfsLiteralRelativePath,
      nodeKind: GitFsNodeKind,
  ): Classification?
}

fun GitWorktreeFilter.classifyEffectively(
    path: UfsLiteralRelativePath,
    nodeKind: GitFsNodeKind,
): GitWorktreeFilter.Classification =
    classify(path = path, nodeKind = nodeKind) ?: GitWorktreeFilter.Classification.Include

fun GitWorktreeFilter.nest(
    directoryName: String,
): GitWorktreeFilter {
  val baseFilter = this

  return object : GitWorktreeFilter {
    override fun classify(
        path: UfsLiteralRelativePath,
        nodeKind: GitFsNodeKind,
    ): GitWorktreeFilter.Classification? =
        baseFilter.classify(
            path = UfsRelativePath.of(listOf(UfsName.Literal(directoryName)) + path.names),
            nodeKind = nodeKind,
        )
  }
}

fun GitWorktreeFilter.chain(
    baseFilter: GitWorktreeFilter,
): GitWorktreeFilter {
  val innerFilter = this

  return object : GitWorktreeFilter {
    override fun classify(
        path: UfsLiteralRelativePath,
        nodeKind: GitFsNodeKind,
    ): GitWorktreeFilter.Classification? =
        innerFilter.classify(path, nodeKind) ?: baseFilter.classify(path, nodeKind)
  }
}
