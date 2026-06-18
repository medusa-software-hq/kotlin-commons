package software.medusa.commons.git.tree

import software.medusa.commons.git.GitCommitHash

@JvmInline
value class GitTreeSubmoduleLink(
    val commitHash: GitCommitHash,
) : GitTreeLeaf
