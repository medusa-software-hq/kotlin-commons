package software.medusa.commons.git

import java.nio.file.Path
import org.eclipse.jgit.storage.file.FileRepositoryBuilder

class GitRepository(
    private val jRepository: org.eclipse.jgit.lib.Repository,
) {
  val path: Path
    get() = jRepository.workTree.toPath()

  companion object {
    fun open(
        path: Path,
    ): GitRepository =
        GitRepository(
            jRepository =
                FileRepositoryBuilder().setWorkTree(path.toFile()).setMustExist(true).build(),
        )
  }

  suspend fun <T> process(
      block: suspend GitSession.() -> T,
  ): T =
      jRepository.newObjectReader().use { jObjectReader ->
        jRepository.newObjectInserter().use { jObjectInserter ->
          GitSession(
                  jRepository = jRepository,
                  jObjectReader = jObjectReader,
                  jObjectInserter = jObjectInserter,
              )
              .block()
        }
      }
}
